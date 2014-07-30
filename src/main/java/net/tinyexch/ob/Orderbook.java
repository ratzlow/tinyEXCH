package net.tinyexch.ob;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Captures all orders for a given {@link net.tinyexch.ob.Listing}. It can be run in different
 * {@link net.tinyexch.ob.MarketModel}
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 */
public class Orderbook {

    //------------------------------------------------------------------------------------------------------------------
    // mutable state changing during runtime
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Contains all bid/buy orders
     */
    private final Deque<Order> buy = new ArrayDeque<Order>();

    /**
     * Contains all ask/sell orders
     */
    private final Deque<Order> sell = new ArrayDeque<Order>();


    //------------------------------------------------------------------------------------------------------------------
    // mutable state changing during runtime
    //------------------------------------------------------------------------------------------------------------------

    // TODO (FRa) : (FRa) : form proper Notif Msg e.g. NewAck, CancelAcc, CancelRej
    public void submit( Order order ) {
        // pre condition
        Objects.requireNonNull(order, "Order must not be null!");
        add( order );
    }

    //------------------------------------------------------------------------------------------------------------------
    // mutable state changing during runtime
    //------------------------------------------------------------------------------------------------------------------

    private void add( Order order ) {
        if ( order.getSide() == Side.BUY ) {
            buy.add( order );
        } else {
            sell.add( order );
        }
    }

}
