package net.tinyexch.ob;

import net.tinyexch.ob.match.Match;
import net.tinyexch.ob.match.MatchEngine;
import net.tinyexch.ob.match.Priorities;
import net.tinyexch.order.Order;
import net.tinyexch.order.Side;

import java.util.Objects;
import java.util.stream.Stream;

import static net.tinyexch.ob.SubmitType.*;
import static net.tinyexch.ob.match.MatchEngine.*;

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
// TODO (FRa) : (FRa) : cancel(): remove from OB and produce CXL-ACK (check FIX what is the response)
public class Orderbook {

    private MatchEngine matchEngine = MatchEngine.NO_OP;
    private long sequence = 0;

    //------------------------------------------------------------------------------------------------------------------
    // mutable state changing during runtime
    //------------------------------------------------------------------------------------------------------------------

    private OrderbookState state = OrderbookState.CLOSED;

    /**
     * Contains all bid/buy orders
     */
    private final OrderbookSide buySide =
            new OrderbookSide( Side.BUY, MatchEngine.BUY_PRICE_TIME_ORDERING, BUY_STOPPRICE_ORDERING );

    /**
     * Contains all ask/sell orders
     */
    private final OrderbookSide sellSide =
            new OrderbookSide( Side.SELL, MatchEngine.SELL_PRICE_TIME_ORDERING, SELL_STOPPRICE_ORDERING );


    //------------------------------------------------------------------------------------------------------------------
    // constructors
    //------------------------------------------------------------------------------------------------------------------

    public Orderbook() {}

    public Orderbook( Order[] buys, Order[] sells ) {
        Objects.requireNonNull(buys, "No buy orders specified!");
        Objects.requireNonNull(sells, "No sell orders specified!");

        Stream.of(buys).forEach( buySide::add );
        Stream.of(sells).forEach( sellSide::add );
    }

    public Orderbook( MatchEngine matchEngine ) {
        this.matchEngine = matchEngine;
    }

    //------------------------------------------------------------------------------------------------------------------
    // mutable state changes during runtime
    //------------------------------------------------------------------------------------------------------------------

    public Match submit( Order order, SubmitType submitType ) {
        // pre conditions
        Objects.requireNonNull(order, "Order must not be null!");

        if ( state == OrderbookState.CLOSED ) {
            String msg = String.format("Cannot accept orders while the orderbook is closed! SubmitType=%s, order=%s",
                    submitType, order.toString());
            throw new OrderbookException( msg );
        }

        final Match match;
        if ( submitType == NEW ) {
            match = match(order);

        } else if ( submitType == MODIFY ) {
            cancel(order);
            match = match(order);

        } else if ( submitType == CANCEL ) {
            cancel(order);
            match = Match.NO_MATCH;

        } else {
            throw new OrderbookException("Invalid submit type " + submitType + " for " + order );
        }

        return match;
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

    private void cancel(Order order) { sameSide(order.getSide()).cancel(order); }

    private Match match(Order order) {
        Side incomingSide = order.getSide();
        OrderbookSide thisSide = sameSide(incomingSide);
        OrderbookSide otherSide = oppositeSide(incomingSide);

        Match match = matchEngine.match(order, otherSide, thisSide);

        // TODO (FRa) : (FRa) : check round/odd lots handling
        boolean fullyMatched = order.getLeavesQty() == 0;
        if ( !fullyMatched && match.getState() == Match.State.ACCEPT ) {
            thisSide.add( order.setSubmitSequence( ++sequence ) );
        }

        return match;
    }

    private OrderbookSide oppositeSide(Side incomingSide) {
        return incomingSide == Side.BUY ? sellSide : buySide;
    }

    private OrderbookSide sameSide(Side incomingSide) {
        return incomingSide == Side.BUY ? buySide : sellSide;
    }

    public OrderbookSide getBuySide() { return buySide; }

    public OrderbookSide getSellSide() { return sellSide; }
}