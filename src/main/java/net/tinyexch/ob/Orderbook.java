package net.tinyexch.ob;

import net.tinyexch.ob.match.MatchEngine;
import net.tinyexch.order.Order;
import net.tinyexch.order.Side;
import net.tinyexch.order.Trade;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

import static net.tinyexch.ob.SubmitType.CANCEL;
import static net.tinyexch.ob.SubmitType.MODIFY;
import static net.tinyexch.ob.SubmitType.NEW;

/**
 * Captures all orders for a given {@link net.tinyexch.ob.Listing}. It can be run in different
 * {@link net.tinyexch.exchange.market.MarketModel}
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 */
// TODO (FRa) : (FRa) : impl. partial cancel, expiration at close, removal of transient orders
// TODO (FRa) : (FRa) : form proper Notif Msg e.g. NewAck, CancelAcc, CancelRej
// TODO (FRa) : (FRa) : impl 2PC and commmit match only if price checks succeeded; check if this should be drawn into OB to be more efficient
// TODO (FRa) : (FRa) : use persistent/functional data structures - if possible
public class Orderbook {

    private MatchEngine matchEngine = order -> Optional.<Trade>empty();

    //------------------------------------------------------------------------------------------------------------------
    // mutable state changing during runtime
    //------------------------------------------------------------------------------------------------------------------

    private OrderbookState state = OrderbookState.CLOSED;

    /**
     * Contains all bid/buy orders
     */
    private final Deque<Order> buy = new ArrayDeque<Order>();

    /**
     * Contains all ask/sell orders
     */
    private final Deque<Order> sell = new ArrayDeque<Order>();


    //------------------------------------------------------------------------------------------------------------------
    // constructors
    //------------------------------------------------------------------------------------------------------------------


    public Orderbook() {}

    public Orderbook( MatchEngine matchEngine ) {
        this.matchEngine = matchEngine;
    }


    //------------------------------------------------------------------------------------------------------------------
    // mutable state changing during runtime
    //------------------------------------------------------------------------------------------------------------------

    public Optional<Trade> submit( Order order, SubmitType submitType ) {
        // pre condition
        Objects.requireNonNull(order, "Order must not be null!");

        final Optional<Trade> trade;
        if ( submitType == NEW ) {
            trade = match(order);

        } else if ( submitType == MODIFY ) {
            cancel(order);
            trade = match(order);

        } else if ( submitType == CANCEL ) {
            cancel(order);
            trade = Optional.empty();

        } else {
            throw new OrderbookException("Invalid submit type " + submitType + " for " + order );
        }

        return trade;
    }


    //------------------------------------------------------------------------------------------------------------------
    // manipulate the the OB
    //------------------------------------------------------------------------------------------------------------------

    public void open() {
        state = OrderbookState.OPEN;
    }

    public void close() {
        state = OrderbookState.CLOSED;
    }

    public void closePartially() {
        state = OrderbookState.PARTIALLY_CLOSED;
    }

    public OrderbookState getState() { return state; }

    //------------------------------------------------------------------------------------------------------------------
    // internal operations
    //------------------------------------------------------------------------------------------------------------------

    private void add( Order order ) {
        if ( order.getSide() == Side.BUY ) {
            buy.add( order );
        } else {
            sell.add( order );
        }
    }

    // TODO (FRa) : (FRa) : remove from OB and produce CXL-ACK (check FIX what is the response)
    private Optional<Trade> cancel( Order order ) {
        throw new IllegalStateException("Not yet implemented!");
    }

    private Optional<Trade> match( Order order ) {
        return matchEngine.match(order);
    }
}