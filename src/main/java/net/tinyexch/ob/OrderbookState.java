package net.tinyexch.ob;

/**
 * Marks the state of an {@link net.tinyexch.ob.Orderbook} that determines what actions the book might allow.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-02
 */
public enum OrderbookState {
    OPEN,
    PARTIALLY_CLOSED,
    CLOSED
}
