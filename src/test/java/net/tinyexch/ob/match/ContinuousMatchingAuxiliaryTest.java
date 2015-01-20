package net.tinyexch.ob.match;

import net.tinyexch.ob.Orderbook;
import net.tinyexch.ob.TestConstants;
import net.tinyexch.ob.price.safeguard.VolatilityInterruptionGuard;
import net.tinyexch.order.ExecType;
import net.tinyexch.order.Order;
import net.tinyexch.order.OrderType;
import net.tinyexch.order.Trade;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.tinyexch.ob.SubmitType.NEW;
import static net.tinyexch.ob.match.OrderFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        Orderbook ob = new Orderbook(new ContinuousMatchEngine(referencePrice, VolatilityInterruptionGuard.NO_OP));
        ob.open();
        int shareNo = 1_000;
        for (int i = 0; i < shareNo; i++ ) {
            ob.submit(buyM(1), NEW);
        }

        Long buyQty = ob.getBuySide().getOrders().stream().collect(Collectors.summingLong(Order::getOrderQty));
        assertEquals(shareNo, buyQty.longValue());

        Order bigSellOrder = sellM(shareNo);
        List<Trade> trades = ob.submit(bigSellOrder, NEW).getTrades();
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

    /**
     * Any unexecuted part of a {@link net.tinyexch.order.OrderType#MARKET_TO_LIMIT} order is entered into the order
     * book with a limit equal to the price of the first partial execution.
     */
    @Test
    public void testUnexecutedMtoLKeptInOrderbook() {
        double referencePrice = 100;
        Orderbook ob = new Orderbook( new ContinuousMatchEngine(referencePrice, VolatilityInterruptionGuard.NO_OP) );
        ob.open();
        int noStandingBuyOrders = 10;
        final double bestBuyPrice = 100.0;
        for ( int i=0; i < noStandingBuyOrders; i++ ) {
            double price = bestBuyPrice - i;
            List<Trade> trades = ob.submit(buyL(price, 1), NEW).getTrades();
            assertTrue( trades.isEmpty() );
        }
        Collection<Order> buyOrders = ob.getBuySide().getOrders();
        assertEquals(noStandingBuyOrders, buyOrders.size());

        int totalBuyQty = buyOrders.stream().collect(Collectors.summingInt(Order::getOrderQty));
        assertEquals( noStandingBuyOrders, totalBuyQty);

        Set<Double> buyPrices = buyOrders.stream().map(Order::getPrice).distinct().collect(Collectors.toSet());
        assertEquals( noStandingBuyOrders, buyPrices.size() );
        assertTrue( "Lowest buy price is the start price!", buyOrders.stream().allMatch(o -> o.getPrice() <= bestBuyPrice) );
        assertEquals( 0, ob.getSellSide().getOrders().size() );

        int sellQty = noStandingBuyOrders * 2;
        Order incoming = sellMtoL(sellQty);
        List<Trade> trades = ob.submit(incoming, NEW).getTrades();
        assertEquals( "All standing orders are executed!", noStandingBuyOrders, trades.size() );

        Trade firstTrade = trades.get(0);
        assertEquals(ExecType.TRADE, firstTrade.getExecType());
        assertEquals(1, firstTrade.getExecutionQty());
        assertEquals( bestBuyPrice, firstTrade.getPrice(), TestConstants.ROUNDING_DELTA );

        assertEquals("All buy orders are executed", 0, ob.getBuySide().getOrders().size());
        assertEquals("Unexecuted part added as new LIMIT order", 1, ob.getSellSide().getOrders().size() );
        Order remainingOrder = ob.getSellSide().getLimitOrders().peek();
        assertEquals( incoming.getClientOrderID(), remainingOrder.getClientOrderID() );
        assertEquals( OrderType.LIMIT, remainingOrder.getOrderType() );
        assertEquals( bestBuyPrice, remainingOrder.getPrice(), TestConstants.ROUNDING_DELTA );
    }
}
