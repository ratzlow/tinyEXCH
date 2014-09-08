package net.tinyexch.exchange.schedule;

import net.tinyexch.exchange.trading.form.TradingModelStateChanger;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.Optional;

/**
 * Configuration item for a trading time slice. Immutable and thread safe.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-06
 */
public final class TradingPhaseTrigger {

    /**
     * Specifies how to interpret this trigger in relation to it is predecessor
     */
    public static enum InitiatorType {
        /** Kick off only after a given state change occurred as a signal */
        WAIT_FOR_STATECHANGE,

        /** Kick off this state change at a given time. */
        FIXED_TIME
    }

    //
    // instance vars
    //

    private final TradingModelStateChanger stateChanger;
    private final InitiatorType initiatorType;
    private Optional<LocalTime> fixedTime = Optional.empty();
    private Optional<Enum> waitFor = Optional.empty();

    //
    // constructor
    //

    public TradingPhaseTrigger(TradingModelStateChanger stateChanger, LocalTime time ) {
        this.stateChanger = stateChanger;
        this.initiatorType = InitiatorType.FIXED_TIME;
        this.fixedTime = Optional.of(time);
    }

    public TradingPhaseTrigger(TradingModelStateChanger stateChanger, Enum waitFor) {
        this.stateChanger = stateChanger;
        this.initiatorType = InitiatorType.WAIT_FOR_STATECHANGE;
        this.waitFor = Optional.of(waitFor);
    }

    //
    // public API
    //

    public TradingModelStateChanger getStateChanger() {
        return stateChanger;
    }

    public LocalTime getFixedTime() {
        return fixedTime.orElseThrow( () -> new SchedulerException("You are dealing with a WaitFor trigger!") );
    }

    public Enum getWaitFor() {
        return waitFor.orElseThrow( () -> new SchedulerException("You are dealing with a FixedTime trigger!") );
    }

    public InitiatorType getInitiatorType() {
        return initiatorType;
    }

    public Duration getDurationToFire(LocalTime from) {
        if ( !fixedTime.isPresent() || fixedTime.get().isBefore(from) ) {
            String msg = String.format("Cannot start auction in the past! from=%s fixedTime=%s",
                    from.toString(), fixedTime.toString());
            throw new SchedulerException(msg + " " + toString());
        }
        long offsetMillis = fixedTime.get().getLong(ChronoField.MILLI_OF_DAY) - from.getLong(ChronoField.MILLI_OF_DAY);

        assert (offsetMillis >= 0);
        return Duration.ofMillis( offsetMillis );
    }

    @Override
    public String toString() {
        return "TradingPhaseTrigger{" +
                "stateChanger=" + stateChanger +
                ", initiatorType=" + initiatorType +
                ", fixedTime=" + fixedTime +
                ", waitFor=" + waitFor +
                '}';
    }
}
