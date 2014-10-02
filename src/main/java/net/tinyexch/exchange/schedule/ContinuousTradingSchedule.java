package net.tinyexch.exchange.schedule;

import net.tinyexch.exchange.trading.model.TradingFormRunType;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState.RUNNING;
import static net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState.STOPPED;

/**
 * This class generates the needed trading phase triggers for an auction.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-31
 */
public class ContinuousTradingSchedule implements TradingFormSchedule {

    private final LocalTime start;
    private final LocalTime stop;

    public ContinuousTradingSchedule(LocalTime start, LocalTime stop) {
        this.start = start;
        this.stop = stop;
    }

    @Override
    public List<TradingPhaseTrigger> getTriggers() {
        List<TradingPhaseTrigger> triggers = new ArrayList<>();

        // start call phase at fixed time
        triggers.add( new TradingPhaseTrigger(RUNNING, start, TradingFormRunType.CONTINUOUS_TRADING) );
        triggers.add( new TradingPhaseTrigger(STOPPED, stop) );

        return triggers;
    }

}
