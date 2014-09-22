package net.tinyexch.ob;

/**
 * Thrown if orders cannot be properly processed in an orderbook.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-18
 */
public class OrderbookException extends RuntimeException {
    public OrderbookException(String message) {
        super(message);
    }
}
