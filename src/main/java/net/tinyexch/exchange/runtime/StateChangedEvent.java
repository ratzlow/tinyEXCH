package net.tinyexch.exchange.runtime;

/**
 * // TODO (FRa) : (FRa) : comment
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-22
 */
public class StateChangedEvent<S extends Enum<S>> implements MarketNotification {

    private final S previous;
    private final S current;


    public StateChangedEvent(S previous, S currentState) {
        this.previous = previous;
        this.current = currentState;
    }

    @Override
    public void process() { }

    public S getPrevious() {
        return previous;
    }

    public S getCurrent() {
        return current;
    }
}
