package net.tinyexch.exchange.schedule;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * This class generates the needed trading phase triggers for an auction.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-31
 */
public class ContinuousTradingSchedule implements TradingFormSchedule {

    private final LocalTime start;
    private final LocalTime stop;
    private final TradingFormInitializer initializer;

    public ContinuousTradingSchedule(TradingFormInitializer initializer, LocalTime start, LocalTime stop) {
        this.initializer = initializer;
        this.start = start;
        this.stop = stop;
    }

    @Override
    public List<TradingPhaseTrigger> getTriggers() {
        List<TradingPhaseTrigger> triggers = new ArrayList<>();

        // start call phase at fixed time
        triggers.add( new TradingPhaseTrigger(StateChangerFactory.startContinuousTrading(), start) );
        triggers.add( new TradingPhaseTrigger(StateChangerFactory.stopContinuousTrading(), stop) );

        return triggers;
    }

    @Override
    public TradingFormInitializer getInitializer() { return initializer; }
}
