package net.tinyexch.ob.match;

import net.tinyexch.ob.Orderbook;
import net.tinyexch.order.Order;
import net.tinyexch.order.Trade;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static net.tinyexch.ob.SubmitType.NEW;
import static net.tinyexch.ob.match.OrderFactory.*;

/**
 * Test code against sample orderbook constellations as described in chap 13.2.2
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-23
 */
public class ContinuousMatchTest {

    /**
     * A market order meets an order book with market orders only on the other side of the order book.
     */
    @Test
    public void testOnlyMarketOrdersOnOtherSide_Ex1() {
        double referencePrice = 200;
        int orderQty = 6000;

        MatchEngine matchEngine = new ContinuousMatchEngine(referencePrice);
        // orderbook is empty
        Orderbook ob = new Orderbook(matchEngine);
        Order standingMarketOrder = buyM(orderQty, time(9, 1, 0));
        List<Trade> emptyTrade = ob.submit(standingMarketOrder, NEW);

        // no trade generated as there is nothing to match
        Assert.assertTrue( emptyTrade.isEmpty() );

        // order recorded in the book
        assertEquals( 1, ob.getBuySide().getOrders().size() );
        assertEquals( standingMarketOrder.getClientOrderID(),
                      ob.getBuySide().getOrders().iterator().next().getClientOrderID() );
        assertEquals( 0, ob.getSellSide().getOrders().size() );

        // new order added which should lead to an execution, leaving the OB empty
        Order immediatelyExecutedOrder = sellM(orderQty);
        Trade trade = ob.submit(immediatelyExecutedOrder, NEW).get(0);
        assertEquals( referencePrice, trade.getPrice());
        assertEquals( orderQty, trade.getExecutionQty());
        assertEquals( standingMarketOrder.getClientOrderID(), trade.getBuy().getClientOrderID() );
        assertEquals( immediatelyExecutedOrder.getClientOrderID(), trade.getSell().getClientOrderID() );
        assertEquals( "sell side", 0, ob.getSellSide().getOrders().size() );
        assertEquals( "buy side", 0, ob.getBuySide().getOrders().size() );
    }
}
