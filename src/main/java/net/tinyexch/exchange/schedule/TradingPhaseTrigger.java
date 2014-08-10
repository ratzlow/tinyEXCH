package net.tinyexch.exchange.schedule;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Configuration item for a trading time slice.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-06
 */
public class TradingPhaseTrigger<S extends Enum<S>> {

    //
    // instance vars
    //

    private Optional<LocalTime> startAt = Optional.empty();
    private Optional<Duration> duration = Optional.empty();

    private final S targetState;

    //
    // constructor
    //

    public TradingPhaseTrigger(S targetState, Duration duration ) {
        this.targetState = targetState;
        this.duration = Optional.of(duration);
    }

    public TradingPhaseTrigger(S targetState, LocalTime startAt ) {
        this.startAt = Optional.of(startAt);
        this.targetState = targetState;
    }

    //
    // accessors
    //

    public Optional<LocalTime> getStartAt() {
        return startAt;
    }

    public Optional<Duration> getDuration() {
        return duration;
    }

    public long delayInMillisFrom( LocalTime time ) {
        return 0; // TODO (FRa) : (FRa) : impl
    }

    public S getTargetState() {
        return targetState;
    }
}
