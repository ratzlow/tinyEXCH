package net.tinyexch.ob.match;

import net.tinyexch.ob.Orderbook;
import net.tinyexch.ob.SubmitType;
import net.tinyexch.ob.TestConstants;
import net.tinyexch.order.Order;
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

    @Before
    public void init() {
        ob = new Orderbook(matchEngine);
    }

    /**
     * A market order meets an order book with market orders only on the other side of the order book.
     */
    @Test
    public void testOnlyMarketOrdersOnOtherSide_Ex1() {
        Order standingOrder = buyM(orderQty, time(9, 1, 0));
        matchIncomingMarketOrder(standingOrder, referencePrice);
    }

    /**
     * Both orders are executed at the highest bid limit of € 201.
     */
    @Test
    public void testOnlyLimitOrderOnOtherSide_Ex2() {
        int highestLimitPrice = 201;
        Order standingOrder = buyL(highestLimitPrice, orderQty, time(9, 1, 0));
        matchIncomingMarketOrder(standingOrder, highestLimitPrice);
    }

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
        assertEquals( shareNo, buyOrderIDs.size() );
    }


    private void matchIncomingMarketOrder( Order standingOrder, double expectedExecutionPrice ) {
        List<Trade> emptyTrade = ob.submit(standingOrder, NEW);

        // no trade generated as there is nothing to match
        assertTrue(emptyTrade.isEmpty());

        // order recorded in the book
        assertEquals( 1, ob.getBuySide().getOrders().size() );
        assertEquals( standingOrder.getClientOrderID(),
                      ob.getBuySide().getOrders().iterator().next().getClientOrderID() );
        assertEquals( 0, ob.getSellSide().getOrders().size() );

        // new order added which should lead to an execution, leaving the OB empty
        Order incomingOrder = sellM(orderQty);
        List<Trade> trades = ob.submit(incomingOrder, NEW);
        assertEquals( 1, trades.size() );
        Trade trade = trades.get(0);
        assertEquals( expectedExecutionPrice, trade.getPrice(), TestConstants.ROUNDING_DELTA);
        assertEquals( orderQty, trade.getExecutionQty());
        assertEquals( standingOrder.getClientOrderID(), trade.getBuy().getClientOrderID() );
        assertEquals( incomingOrder.getClientOrderID(), trade.getSell().getClientOrderID() );
        assertEquals( "incoming buy side", 0, ob.getBuySide().getOrders().size() );
        assertEquals( "standing sell side", 0, ob.getSellSide().getOrders().size() );
    }
}
