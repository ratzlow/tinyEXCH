package net.tinyexch.ob;

import net.tinyexch.ob.match.Match;
import net.tinyexch.ob.match.MatchEngine;
import net.tinyexch.ob.match.Priorities;
import net.tinyexch.order.Order;
import net.tinyexch.order.Side;
import net.tinyexch.order.Trade;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
public class Orderbook {

    private MatchEngine matchEngine = MatchEngine.NO_OP;

    //------------------------------------------------------------------------------------------------------------------
    // mutable state changing during runtime
    //------------------------------------------------------------------------------------------------------------------

    private OrderbookState state = OrderbookState.CLOSED;

    /**
     * Contains all bid/buy orders
     */
    private final OrderbookSide buySide =
            new OrderbookSide( Side.BUY, BUY_PRICE_ORDERING, Priorities.TIME, BUY_STOPPRICE_ORDERING );

    /**
     * Contains all ask/sell orders
     */
    private final OrderbookSide sellSide =
            new OrderbookSide( Side.SELL, SELL_PRICE_ORDERING, Priorities.TIME, SELL_STOPPRICE_ORDERING );


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
    // mutable state changing during runtime
    //------------------------------------------------------------------------------------------------------------------

    public List<Trade> submit( Order order, SubmitType submitType ) {
        // pre condition
        Objects.requireNonNull(order, "Order must not be null!");

        final List<Trade> trades;
        if ( submitType == NEW ) {
            trades = match(order).getTrades();

        } else if ( submitType == MODIFY ) {
            cancel(order);
            trades = match(order).getTrades();

        } else if ( submitType == CANCEL ) {
            cancel(order);
            trades = Collections.emptyList();

        } else {
            throw new OrderbookException("Invalid submit type " + submitType + " for " + order );
        }

        return trades;
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

    // TODO (FRa) : (FRa) : remove from OB and produce CXL-ACK (check FIX what is the response)
    private Optional<Trade> cancel( Order order ) {
        throw new IllegalStateException("Not yet implemented!");
    }

    private Match match(Order order) {
        final OrderbookSide thisSide;
        final OrderbookSide otherSide;
        if ( order.getSide() == Side.BUY ) {
            thisSide = buySide;
            otherSide = sellSide;
        } else {
            thisSide = sellSide;
            otherSide = buySide;
        }

        Match match = matchEngine.match( order, otherSide );
        // TODO (FRa) : (FRa) : check round/odd lots handling
        boolean fullyMatched = order.getLeavesQty() - match.getExecutedQuantity() == 0;
        if ( !fullyMatched && match.getState() == Match.State.ACCEPT ) {
            thisSide.add( order );
        }

        return match;
    }

    public OrderbookSide getBuySide() { return buySide; }

    public OrderbookSide getSellSide() { return sellSide; }
}