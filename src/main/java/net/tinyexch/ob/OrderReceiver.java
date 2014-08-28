package net.tinyexch.ob;

import net.tinyexch.order.Order;

/**
 * Implementers can deal with order request submitted to the order book. The request type is specified by
 * the {@link net.tinyexch.ob.SubmitType}
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-28
 */
public interface OrderReceiver {
    void submit( Order order, SubmitType submitType );
}
