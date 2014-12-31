package net.tinyexch.ob.match;

import net.tinyexch.ob.Orderbook;
import net.tinyexch.ob.SubmitType;
import net.tinyexch.ob.TestConstants;
import net.tinyexch.order.Order;
import net.tinyexch.order.Trade;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.tinyexch.ob.match.OrderFactory.buyM;
import static net.tinyexch.ob.match.OrderFactory.sellM;
import static org.junit.Assert.assertEquals;

/**
 * Number of test cases not specifically described in the specs but reflecting various orderbook constellations.
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-31
 */
// TODO (FRa) : (FRa) : port to groovy
public class ContinuousMatchingAuxiliaryTest {


    @Test
    public void testOrderWithManyPartialExecutions() {
        final int referencePrice = 200;
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
}
