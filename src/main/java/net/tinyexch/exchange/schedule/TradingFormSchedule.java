package net.tinyexch.exchange.schedule;

import java.util.List;

/**
 * Implementations can generate triggers for their specific trading form. First trigger needs to be a fix timed trigger
 * that will setup the trading form using the {@link net.tinyexch.exchange.schedule.TradingFormInitializer}
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-31
 */
public interface TradingFormSchedule {

    /**
     * @return list of triggers with first trigger being a fixed time trigger
     */
    List<TradingPhaseTrigger> getTriggers();
}
