package net.tinyexch.ob;

/**
 * Simultaneous entry of a buy and sell limit order with a {@link net.tinyexch.ob.TimeInForce#DAY}
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 */
public class Quote {
    private final TimeInForce timeInForce = TimeInForce.DAY;
    private Order sell;
    private Order buy;
}
