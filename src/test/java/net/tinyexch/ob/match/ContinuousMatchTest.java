package net.tinyexch.ob.match;

import net.tinyexch.ob.Orderbook;
import net.tinyexch.ob.OrderbookSide;
import net.tinyexch.ob.SubmitType;
import net.tinyexch.ob.TestConstants;
import net.tinyexch.order.Order;
import net.tinyexch.order.Side;
import net.tinyexch.order.Trade;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static net.tinyexch.ob.SubmitType.NEW;
import static net.tinyexch.ob.match.OrderFactory.*;

/**
 * Test code against sample orderbook constellations as described in chap 13.2.2
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-23
 */
public class ContinuousMatchTest {

    private final double referencePrice = 200;
    private final MatchEngine matchEngine = new ContinuousMatchEngine(referencePrice);
    private final int orderQty = 6000;
    private Orderbook ob;




    // TODO (FRa) : (FRa) : port to groovy
    @Test
    public void testOrderWithManyPartialExecutions() {
        Orderbook ob = new Orderbook(new ContinuousMatchEngine(referencePrice));
        int shareNo = 1_000;
        Stream.iterate(0, i -> i++).limit(shareNo).forEach( i -> ob.submit(buyM(1), SubmitType.NEW) );
        Long buyQty = ob.getBuySide().getOrders().stream().collect(Collectors.summingLong(Order::getOrderQty));
        assertEquals(shareNo, buyQty.longValue());

        Order bigSellOrder = sellM(shareNo);
        List<Trade> trades = ob.submit(bigSellOrder, SubmitType.NEW);
        assertEquals( shareNo, trades.size() );
        assertEquals( 0, ob.getBuySide().getOrders().size() );
        assertEquals( 0, ob.getSellSide().getOrders().size() );
        for (Trade trade : trades) {
            assertEquals(1, trade.getExecutionQty());
            assertEquals(referencePrice, trade.getPrice(), TestConstants.ROUNDING_DELTA);
            assertEquals(bigSellOrder.getClientOrderID(), trade.getSell().getClientOrderID());
        }
        Set<String> buyOrderIDs = trades.stream().map(trade -> trade.getBuy().getClientOrderID()).collect(Collectors.toSet());
        assertEquals(shareNo, buyOrderIDs.size());
    }


    @Before
    public void init() {
        ob = new Orderbook(matchEngine);
    }

    /**
     * A market order meets an order book with market orders only on the other side of the order book.
     * The reference price is € 200. Both market orders are executed at the reference price of € 200 (see principle 1).
     */
    @Test
    public void testOnlyMarketOrdersOnOtherSide_Ex1() {
        Order standingOrder = buyM(orderQty, time(9, 1, 0));
        Order incomingOrder = sellM(orderQty);
        matchIncomingMarketOrder(standingOrder, incomingOrder, referencePrice);
    }

    /**
     * A market order meets an order book with limit orders only on the other side of the order book.
     * Both orders are executed at the highest bid limit of € 201.
     */
    @Test
    public void testOnlyBuyLimitOrderOnOtherSide_Ex2() {
        int highestBidLimit = 201;
        Order standingOrder = buyL(highestBidLimit, orderQty, time(9, 1, 0));
        Order incomingOrder = sellM(orderQty);
        matchIncomingMarketOrder(standingOrder, incomingOrder, highestBidLimit);
    }

    /**
     * A market order meets an order book with limit orders only on the other side of the order book.
     * Both orders are executed at the lowest ask limit of € 199
     */
    @Test
    public void testOnlyAskLimitOrderOnOtherSide_Ex3() {
        int lowestAskLimit = 199;
        Order standingOrder = sellL(lowestAskLimit, orderQty, time(9, 1, 0));
        Order incomingOrder = buyM(orderQty);
        matchIncomingMarketOrder(standingOrder, incomingOrder, lowestAskLimit);
    }

    /**
     * A market order meets an order book with market orders and limit orders on the other side of the order book.
     * The incoming ask market order is executed against the bid market order in the order book at the reference
     * price of € 200 (see principle 1)
     */
    @Test
    public void testMarketAndLimitOrderOnOtherSide_Ex4() {
        Order standingMarket = buyM(orderQty, time(9, 1, 0));
        Order standingLimit = buyL(195D, orderQty, time(9, 1, 0));
        ob.submit(standingMarket, SubmitType.NEW);
        ob.submit(standingLimit, SubmitType.NEW);

        Order incomingOrder = sellM(orderQty);
        List<Trade> trades = ob.submit(incomingOrder, SubmitType.NEW);
        assertEquals( 1, trades.size() );
        assertEquals( 1, ob.getBuySide().getOrders().size() );
        assertEquals( 0, ob.getSellSide().getOrders().size() );

        Trade trade = trades.get(0);
        assertEquals( referencePrice, trade.getPrice(), TestConstants.ROUNDING_DELTA );
        assertEquals( orderQty, trade.getExecutionQty() );
        assertEquals( standingMarket.getClientOrderID(), trade.getBuy().getClientOrderID() );
        assertEquals( incomingOrder.getClientOrderID(), trade.getSell().getClientOrderID() );
    }


    private void matchIncomingMarketOrder( Order standingOrder, Order incomingOrder, double expectedExecutionPrice ) {
        List<Trade> emptyTrade = ob.submit(standingOrder, NEW);

        // no trade generated as there is nothing to match
        assertTrue(emptyTrade.isEmpty());

        final Order buy, sell;
        final OrderbookSide incomingSide, standingSide;
        if (standingOrder.getSide() == Side.BUY) {
            standingSide = ob.getBuySide();
            incomingSide = ob.getSellSide();
            buy = standingOrder;
            sell = incomingOrder;
        } else {
            standingSide = ob.getSellSide();
            incomingSide = ob.getBuySide();
            buy = incomingOrder;
            sell = standingOrder;
        }

        // order recorded in the book
        assertEquals( 1, standingSide.getOrders().size() );
        assertEquals( standingOrder.getClientOrderID(),
                      standingSide.getOrders().iterator().next().getClientOrderID() );
        assertEquals( 0, incomingSide.getOrders().size() );

        // new order added which should lead to an execution, leaving the OB empty

        List<Trade> trades = ob.submit(incomingOrder, NEW);
        assertEquals( 1, trades.size() );
        Trade trade = trades.get(0);
        assertEquals( expectedExecutionPrice, trade.getPrice(), TestConstants.ROUNDING_DELTA);
        assertEquals( orderQty, trade.getExecutionQty());
        assertEquals( buy.getClientOrderID(), trade.getBuy().getClientOrderID() );
        assertEquals( sell.getClientOrderID(), trade.getSell().getClientOrderID() );
        assertEquals( "incoming buy side", 0, ob.getBuySide().getOrders().size() );
        assertEquals( "standing sell side", 0, ob.getSellSide().getOrders().size() );
    }
}
