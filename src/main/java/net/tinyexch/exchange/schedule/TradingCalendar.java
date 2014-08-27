package net.tinyexch.exchange.schedule;

import net.tinyexch.exchange.trading.form.auction.AuctionState;
import net.tinyexch.exchange.trading.form.auction.AuctionStateChange;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The calender defines the trading
 * - days as day in year,
 * - the time slots for trading on those particular days
 * - the kind of trading form scheduled for the a time slot
 *
 * Please be aware that all trading days share the same trading times and forms.
 *
 *
 * Auction:
 *  Start::CALL_RUNNING -> StartTime+Duration
 *  CALL_WITH_RANDOM_END -> bound Duration,
 *  PRICE_DETERMINATION_RUNNING -> Duration,
 *  ORDERBOOK_BALANCING_RUNNING -> Duration
 *
 * ContinuousTra
 *  OPEN ->
 *  CLOSE
 *
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-03
 */
public class TradingCalendar {

    private final Random random = new Random(LocalTime.now().getNano());

    private List<LocalDate> tradingDays = new ArrayList<>();
    private List<TradingPhaseTrigger> triggers = new ArrayList<>();

    /**
     * @param tradingDays
     */
    public TradingCalendar( LocalDate ... tradingDays ) {
        this.tradingDays = Arrays.asList(tradingDays);
    }

    //-------------------------------------------------------------------------------------------------
    // helpers to setup the schedule for trading
    //-------------------------------------------------------------------------------------------------

    public TradingCalendar addWaitTrigger(AuctionStateChange stateChange, AuctionState waitFor) {
        return add( new TradingPhaseTrigger(stateChange, waitFor) );
    }

    public TradingCalendar addFixedTimeTrigger(AuctionStateChange stateChange, LocalTime time) {
        return add( new TradingPhaseTrigger(stateChange, time) );
    }

    public TradingCalendar addVariantDurationTrigger( AuctionStateChange stateChange, LocalTime from,
                                                      int plusMinDuration, int plusMaxDuration, ChronoUnit unit ) {
        int duration = random.nextInt(plusMaxDuration - plusMinDuration) + plusMinDuration;
        LocalTime time = from.plus(duration, unit);
        return add( new TradingPhaseTrigger(stateChange, time) );
    }

    public List<LocalDate> getTradingDays() {
        return tradingDays;
    }

    public List<TradingPhaseTrigger> getTriggers() {
        return triggers;
    }

    //
    // internal API
    //

    private TradingCalendar add(final TradingPhaseTrigger trigger) {
        triggers.add( trigger );
        return this;
    }
}
