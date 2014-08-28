package net.tinyexch.exchange.schedule;

import net.tinyexch.exchange.trading.form.auction.AuctionState;
import net.tinyexch.exchange.trading.form.auction.AuctionStateChange;
import net.tinyexch.exchange.trading.model.AuctionTradingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.groupingBy;
import static net.tinyexch.exchange.schedule.TradingPhaseTrigger.InitiatorType.FIXED_TIME;
import static net.tinyexch.exchange.schedule.TradingPhaseTrigger.InitiatorType.WAIT_FOR_STATECHANGE;

/**
 * Start trading for provide trading model according to trading phase triggers.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-03
 */
public class TradingModelPhaseChanger {
    private final Logger LOGGER = LoggerFactory.getLogger(TradingModelPhaseChanger.class);
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final AuctionTradingModel tradingModel;

    //
    // constructors
    //
    public TradingModelPhaseChanger(AuctionTradingModel tradingModel) {
        this.tradingModel = tradingModel;
    }

    //
    // public API
    //

    public void startTrading( TradingCalendar tradingCalendar ) {
        if ( !isTradingAllowedToday(tradingCalendar) ) { return; }

        LocalTime now = LocalTime.now();
        final Map<TradingPhaseTrigger.InitiatorType, List<TradingPhaseTrigger>> triggersByType =
                tradingCalendar.getTriggers().stream().collect(groupingBy(TradingPhaseTrigger::getInitiatorType));

        // first register the listeners
        if ( triggersByType.containsKey(WAIT_FOR_STATECHANGE) ) {
            triggersByType.get(WAIT_FOR_STATECHANGE).forEach(trigger -> {
                AuctionStateChange stateChange = trigger.getStateChange();
                AuctionState waitFor = trigger.getWaitFor();
                LOGGER.debug("Adding state listener to wait for {} to change to {}", waitFor, stateChange);
                tradingModel.getAuction().register(state -> {
                    LOGGER.debug("Fired listener as state changed to {}", state);
                    if (waitFor == state) {
                        tradingModel.moveTo(stateChange);
                    }
                });
            });
        }

        // now kick off all triggers to run be run in different thread
        if ( triggersByType.containsKey(FIXED_TIME)) {
            triggersByType.get(FIXED_TIME).forEach(trigger -> {
                AuctionStateChange stateChange = trigger.getStateChange();
                Duration duration = trigger.getDurationToFire(now);
                long delayMillis = duration.getNano() / 1_000_000;
                assert (delayMillis > 0);
                executorService.schedule(
                        () -> {
                            LOGGER.info("Start scheduled state change {} ", stateChange);
                            tradingModel.moveTo(stateChange);
                        },
                        delayMillis, TimeUnit.MILLISECONDS
                );
            });
        }
    }

    private boolean isTradingAllowedToday(TradingCalendar tradingCalendar) {
        LocalDate today = LocalDate.now();
        return tradingCalendar.getTradingDays().stream()
                                        .anyMatch(date -> today.compareTo(date) == 0);
    }
}
