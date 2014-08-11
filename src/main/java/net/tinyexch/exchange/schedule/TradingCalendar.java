package net.tinyexch.exchange.schedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * // TODO (FRa) : (FRa) : comment
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-03
 */
public class TradingCalendar {

    private List<LocalDate> tradingDays = new ArrayList<>();
    private List<TradingPhaseTrigger> triggers = new ArrayList<>();

    public List<LocalDate> getTradingDays() {
        return tradingDays;
    }

    public List<TradingPhaseTrigger> getTriggers() {
        return triggers;
    }


    /**
     * Auction:
     *  Start::CALL_RUNNING -> StartTime+Duration
     *  CALL_WITH_RANDOM_END -> bound Duration,
     *  PRICE_DETERMINATION_RUNNING -> Duration,
     *  ORDERBOOK_BALANCING_RUNNING -> Duration
     */


    /**
     * ContinuousTra
     *  OPEN ->
     *  CLOSE
     *
     *
     *
     */


}
