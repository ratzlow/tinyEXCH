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
// TODO (FRa) : (FRa) : impl 2PC and commit match only if price checks succeeded; check if this should be drawn into OB to be more efficient
// TODO (FRa) : (FRa) : use persistent/functional data structures - if possible
// TODO (FRa) : (FRa) : cancel(): remove from OB and produce CXL-ACK (check FIX what is the response)
public class Orderbook {

    private MatchEngine matchEngine = MatchEngine.NO_OP;

    //------------------------------------------------------------------------------------------------------------------
    // mutable state changing during runtime
    //------------------------------------------------------------------------------------------------------------------

    private OrderbookState state = OrderbookState.CLOSED;

    //------------------------------------------------------------------------------------------------------------------
    /**
     * Contains all bid/buy orders
     */
    private final OrderbookSide buySide = new OrderbookSide( Side.BUY,
            MatchEngine.BUY_PRICE_TIME_ORDERING.thenComparing(Priorities.SUBMIT_SEQUENCE),
            BUY_STOPPRICE_ORDERING.thenComparing(Priorities.SUBMIT_SEQUENCE) );

    /**
     * Contains all ask/sell orders
     */
    private final OrderbookSide sellSide = new OrderbookSide( Side.SELL,
            MatchEngine.SELL_PRICE_TIME_ORDERING.thenComparing(Priorities.SUBMIT_SEQUENCE),
            SELL_STOPPRICE_ORDERING.thenComparing(Priorities.SUBMIT_SEQUENCE) );
    //------------------------------------------------------------------------------------------------------------------

    private final OrderbookSide midpointBuySide = new OrderbookSide( Side.BUY, MIDPOINT_COMPARATOR );
    private final OrderbookSide midpointSellSide = new OrderbookSide( Side.SELL, MIDPOINT_COMPARATOR );


    //------------------------------------------------------------------------------------------------------------------
    // constructors
    //------------------------------------------------------------------------------------------------------------------

    public Orderbook() {}

    public Orderbook( Order[] buys, Order[] sells ) {
        Objects.requireNonNull(buys, "No buy orders specified!");
        Objects.requireNonNull(sells, "No sell orders specified!");

        Stream.of(buys).forEach( buySide::add );
        Stream.of(sells).forEach(sellSide::add);
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

    public OrderbookSide getBuySide() { return getBuySide(false); }
    public OrderbookSide getBuySideMidpoint() { return getBuySide(true); }

    public OrderbookSide getSellSide() { return getSellSide(false); }
    public OrderbookSide getSellSideMidpoint() { return getSellSide(true); }


    //------------------------------------------------------------------------------------------------------------------
    // internal operations
    //------------------------------------------------------------------------------------------------------------------

    private void cancel(Order order) { sameSide(order.getSide(), order.isMidpoint()).cancel(order); }

    private Match match(Order order) {
        Side incomingSide = order.getSide();
        OrderbookSide thisSide = sameSide(incomingSide, order.isMidpoint());
        OrderbookSide otherSide = oppositeSide(incomingSide, order.isMidpoint());

        return matchEngine.match(order, otherSide, thisSide);
    }

    private OrderbookSide oppositeSide(Side incomingSide, boolean isMidpoint) {
        return incomingSide == Side.BUY ? getSellSide(isMidpoint) : getBuySide(isMidpoint);
    }

    private OrderbookSide sameSide(Side incomingSide, boolean isMidpoint) {
        return incomingSide == Side.BUY ? getBuySide(isMidpoint) : getSellSide(isMidpoint);
    }

    private OrderbookSide getBuySide( boolean isMidpoint ) {
        return isMidpoint ? midpointBuySide : buySide;
    }

    private OrderbookSide getSellSide( boolean isMidpoint ) {
        return isMidpoint ? midpointSellSide : sellSide;
    }
}