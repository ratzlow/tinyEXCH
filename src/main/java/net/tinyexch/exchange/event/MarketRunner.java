package net.tinyexch.exchange.event;


import net.tinyexch.exchange.event.consume.ConsumerCommandFactory;
import net.tinyexch.exchange.schedule.TradingCalendar;
import net.tinyexch.exchange.schedule.TradingFormSchedule;
import net.tinyexch.exchange.schedule.TradingPhaseTrigger;
import net.tinyexch.exchange.trading.model.TradingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static java.util.stream.Collectors.groupingBy;
import static net.tinyexch.exchange.schedule.TradingPhaseTrigger.InitiatorType.FIXED_TIME;

/**
 * Shell around a market so events can be sent to it and the life cycle of the market will be controlled.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-22
 */
public class MarketRunner {

    private final static Logger LOGGER = LoggerFactory.getLogger(MarketRunner.class);

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor( r -> new Thread(r, "MarketRunner") );
    private final ScheduledExecutorService schedulerService =
            Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "MarketScheduler"));

    private final TradingCalendar tradingCalendar;
    private final ConsumerCommandFactory consumerCommandFactory;


    public MarketRunner( TradingModel market, TradingCalendar tradingCalendar) {
        this.tradingCalendar = tradingCalendar;
        consumerCommandFactory = new ConsumerCommandFactory( market );
    }

    //-----------------------------------------------------------
    // public API
    //-----------------------------------------------------------


    /**
     * Send event to market, so associated event handler can take action on it.
     *
     * @param event to pass to the market
     * @param <T> arbitrary event type
     */
    public <T> void submit( T event ) {
        Runnable cmd = consumerCommandFactory.create( event );
        executorService.submit( cmd );
    }

    //-----------------------------------------------------------
    // life cycle API
    //-----------------------------------------------------------

    /**
     * Start trading on given market.
     *
     * @param notificationListener wired to the market so it can respond to emitted events
     */
    public void start( NotificationListener notificationListener ) {
        if ( !isTradingAllowedToday(tradingCalendar.getTradingDays()) ) { return; }

        notificationListener.init(this, tradingCalendar);
        tradingCalendar.getTradingFormSchedules().forEach(this::initScheduledStateChanges);
    }

    /**
     * Hard shutdown of the market runner and scheduler for timed events.
     */
    public void stop() {
        executorService.shutdown();
        schedulerService.shutdown();
    }

    //-----------------------------------------------------------
    // internal interpretation of scheduled events
    //-----------------------------------------------------------

    /**
     * Register timer for predefined events to change state of the market.
     *
     * @param schedule defined as part of a trading calendar
     */
    private void initScheduledStateChanges(TradingFormSchedule schedule) {
        Map<TradingPhaseTrigger.InitiatorType, List<TradingPhaseTrigger>> triggersByType =
                schedule.getTriggers().stream().collect(groupingBy(TradingPhaseTrigger::getInitiatorType));

        // now kick off all triggers to be run in different thread
        LocalTime now = LocalTime.now();
        if ( triggersByType.containsKey(FIXED_TIME)) {
            List<TradingPhaseTrigger> timedTriggers = triggersByType.get(FIXED_TIME);
            for ( TradingPhaseTrigger trigger : timedTriggers) {
                Duration duration = trigger.getDurationToFire(now);
                long delayMillis = duration.getNano() / 1_000_000;
                assert (delayMillis >= 0);
                LOGGER.info("Submit scheduled events in {} ms", delayMillis);
                schedulerService.schedule(
                        () -> submit(trigger.getChangeStateEvent()),
                        delayMillis, TimeUnit.MILLISECONDS
                );
            }
        }
    }

    /**
     * Check if the trading process might be kicked off today.
     * @param permittedTradingDays containing the allowed trading schedule
     * @return true ... trading allowed today
     */
    private boolean isTradingAllowedToday(List<LocalDate> permittedTradingDays) {
        LocalDate today = LocalDate.now();
        return permittedTradingDays.stream().anyMatch(date -> today.compareTo(date) == 0);
    }
}
