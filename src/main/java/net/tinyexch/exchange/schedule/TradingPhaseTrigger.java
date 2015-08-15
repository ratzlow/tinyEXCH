package net.tinyexch.exchange.schedule;

import net.tinyexch.exchange.event.consume.ChangeStateEvent;
import net.tinyexch.exchange.trading.model.TradingFormRunType;

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

    private ChangeStateEvent changeStateEvent;

    /**
     * Specifies how to interpret this trigger in relation to it is predecessor
     */
    public enum InitiatorType {
        /** Kick off only after a given state change occurred as a signal */
        WAIT_FOR_STATECHANGE,

        /** Kick off this state change at a given time. */
        FIXED_TIME
    }

    //
    // instance vars
    //

    private final InitiatorType initiatorType;
    private Optional<LocalTime> fixedTime = Optional.empty();
    private Optional<Enum> waitFor = Optional.empty();
    private Optional<TradingFormRunType> tradingFormRunType = Optional.empty();


    //
    // constructor
    //


    public TradingPhaseTrigger(Enum targetState, LocalTime time, TradingFormRunType runtype) {
        this.changeStateEvent = new ChangeStateEvent(targetState, runtype );
        this.initiatorType = InitiatorType.FIXED_TIME;
        this.fixedTime = Optional.of(time);
        this.tradingFormRunType = Optional.ofNullable(runtype);
    }

    public TradingPhaseTrigger(Enum targetState, LocalTime time) {
        this(targetState, time, null );
    }

    public TradingPhaseTrigger( Enum targetState, Enum waitFor) {
        this.changeStateEvent = new ChangeStateEvent( targetState );
        this.initiatorType = InitiatorType.WAIT_FOR_STATECHANGE;
        this.waitFor = Optional.of(waitFor);
        this.tradingFormRunType = Optional.empty();
    }


    //
    // public API
    //

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

        assert offsetMillis >= 0;

        return Duration.ofMillis( offsetMillis );
    }


    public ChangeStateEvent getChangeStateEvent() {
        return changeStateEvent;
    }

    @Override
    public String toString() {
        return "TradingPhaseTrigger{" +
                "changeStateEvent=" + changeStateEvent +
                ", initiatorType=" + initiatorType +
                ", fixedTime=" + fixedTime +
                ", waitFor=" + waitFor +
                ", tradingFormRunType=" + tradingFormRunType +
                '}';
    }
}
