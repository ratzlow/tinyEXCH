package net.tinyexch.exchange.event.produce;

import java.time.Instant;

/**
 * Will be emitted when some market life cycle transition occured.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-22
 */
public class StateChangedEvent<S extends Enum<S>>  {

    private final Instant timestamp = Instant.now();
    private final S previous;
    private final S current;


    public StateChangedEvent(S previous, S currentState) {
        this.previous = previous;
        this.current = currentState;
    }

    public S getPrevious() {
        return previous;
    }

    public S getCurrent() {
        return current;
    }

    public Instant getTimestamp() { return timestamp; }
}
