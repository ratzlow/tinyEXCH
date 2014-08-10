package net.tinyexch.order;

/**
 * Simultaneous entry of a buy and sell limit order with a {@link net.tinyexch.order.TimeInForce#DAY}
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 */
public class Quote {
    private final TimeInForce timeInForce = TimeInForce.DAY;
    private Order sell;
    private Order buy;
}
