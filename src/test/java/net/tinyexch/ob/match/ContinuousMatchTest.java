package net.tinyexch.ob.match;

import net.tinyexch.ob.Orderbook;
import net.tinyexch.ob.OrderbookSide;
import net.tinyexch.ob.RejectReason;
import net.tinyexch.ob.TestConstants;
import net.tinyexch.order.ExecType;
import net.tinyexch.order.Order;
import net.tinyexch.order.Side;
import net.tinyexch.order.Trade;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.List;

import static net.tinyexch.ob.SubmitType.NEW;
import static net.tinyexch.ob.match.OrderFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test code against sample orderbook constellations as described in chap 13.2.2
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-23
 */
public class ContinuousMatchTest {

    private static final double REFERENCE_PRICE = 200;
    private static final int ORDER_QTY = 6000;
    private static final MatchEngine MATCH_ENGINE = new ContinuousMatchEngine(REFERENCE_PRICE);

    private Orderbook ob;

    @Before
    public void init() {
        ob = new Orderbook(MATCH_ENGINE);
    }

    //-------------------------------------------------------------------------------------------
    // Scenarios where standing orders face an incoming MARKET order
    //-------------------------------------------------------------------------------------------

    /**
     * A market order meets an order book with market orders only on the other side of the order book.
     * The reference price is € 200. Both market orders are executed at the reference price of € 200 (see principle 1).
     */
    @Test
    public void testOnlyMarketOrdersOnOtherSide_Ex1() {
        Order standingOrder = buyM(ORDER_QTY, time("09:01:00"));
        Order incomingOrder = sellM(ORDER_QTY);
        matchIncomingMarketOrder(standingOrder, incomingOrder, REFERENCE_PRICE);
    }

    /**
     * A market order meets an order book with limit orders only on the other side of the order book.
     * Both orders are executed at the highest bid limit of € 201.
     */
    @Test
    public void testOnlyBuyLimitOrderOnOtherSide_Ex2() {
        int highestBidLimit = 201;
        Order standingOrder = buyL(highestBidLimit, ORDER_QTY, time("09:01:00"));
        Order incomingOrder = sellM(ORDER_QTY);
        matchIncomingMarketOrder(standingOrder, incomingOrder, highestBidLimit);
    }

    /**
     * A market order meets an order book with limit orders only on the other side of the order book.
     * Both orders are executed at the lowest ask limit of € 199
     */
    @Test
    public void testOnlyAskLimitOrderOnOtherSide_Ex3() {
        int lowestAskLimit = 199;
        Order standingOrder = sellL(lowestAskLimit, ORDER_QTY, time("09:01:00"));
        Order incomingOrder = buyM(ORDER_QTY);
        matchIncomingMarketOrder(standingOrder, incomingOrder, lowestAskLimit);
    }

    /**
     * A market order meets an order book with market orders and limit orders on the other side of the order book.
     * The incoming ask market order is executed against the bid market order in the order book at the reference
     * price of € 200.
     * ==> principle 1
     */
    @Test
    public void testStandingMarketAndLimitOrderBestBidBelowRefPrice_Ex4() {
        testStandingMarketAndLimitOrderBid(195D, REFERENCE_PRICE);
    }


    /**
     * A market order meets an order book with market orders and limit orders on the other side of the order book.
     * The reference price is € 200. It is lower than the highest bid limit. The incoming ask market order is executed
     * against the bid market order in the order book at the highest bid limit of € 202.
     * ==> principle 2
     */
    @Test
    public void testStandingMarketAndLimitOrderBestBidAboveRefPrice_Ex5() {
        double standingLimitPrice = 202D;
        testStandingMarketAndLimitOrderBid(standingLimitPrice, standingLimitPrice);
    }


    /**
     * A market order meets an order book with market orders and limit orders on the other side of the order book.
     * The reference price is € 200. It is lower than or equal to the lowest ask limit.
     * The incoming bid market order is executed against the ask market order in the order book at the reference
     * price of € 200.
     * ==> principle 1
     */
    @Test
    public void testStandingMarketAndLimitOrderBestAskAboveRefPrice_Ex6() {
        testStandingMarketAndLimitOrderSell(202, REFERENCE_PRICE);
    }


    /**
     * A market order meets an order book with market orders and limit orders on the other side of the order book.
     * The reference price is € 200. It is higher than the lowest ask limit.
     * The incoming bid market order is executed against the ask market order in the order book at the lowest
     * ask limit of € 195.
     * ==> principle 2
     */
    @Test
    public void testStandingMarketAndLimitOrderBestAskBelowRefPrice_Ex7() {
        int standingLimitPrice = 195;
        testStandingMarketAndLimitOrderSell(standingLimitPrice, standingLimitPrice);
    }


    /**
     * A market order meets an order book in which there are no orders.
     * The incoming bid market order is entered in the order book. A price is not determined and no orders are executed.
     * The order is left in the book
     */
    @Test
    public void testNoOrderOnOtherSide_Ex8() {
        Order standingMarket = buyM(ORDER_QTY);
        List<Trade> trades = ob.submit(standingMarket, NEW);

        assertEquals( 0, trades.size() );
        Collection<Order> allBuyOrders = ob.getBuySide().getOrders();
        assertEquals(1, allBuyOrders.size());
        assertEquals(standingMarket.getClientOrderID(), allBuyOrders.iterator().next().getClientOrderID());
    }


    //-------------------------------------------------------------------------------------------
    // Scenarios where standing orders face an incoming MARKET_TO_LIMIT order
    //-------------------------------------------------------------------------------------------

    /**
     * A market-to-limit order meets an order book with market orders only on the other side of the order book.
     * The market-to-limit order is rejected. A price is not determined and no orders are executed.
     */
    @Test
    public void testMarketOrdersOnlyOnOtherSideRejection_Ex9() {
        Order standingMarket = buyM(ORDER_QTY);
        ob.submit(standingMarket, NEW);
        Runnable checkOrderbook = () -> {
            assertEquals( 0, ob.getSellSide().getOrders().size());
            assertEquals( 1, ob.getBuySide().getOrders().size());
            assertEquals( standingMarket.getClientOrderID(), ob.getBuySide().getOrders().iterator().next().getClientOrderID() );
        };
        // before MtL order is submited
        checkOrderbook.run();

        Order incoming = sellMtoL(ORDER_QTY);
        List<Trade> trades = ob.submit(incoming, NEW);
        Trade trade = checkMtoLOrderRejection(trades);
        assertEquals(incoming.getClientOrderID(), trade.getSell().getClientOrderID());
        assertNull(trade.getBuy());

        // validate nothing has altered the OB
        checkOrderbook.run();
    }


    /**
     * A market-to-limit order meets an order book with limit orders only on the other side of the order book.
     * Both orders are executed at the highest bid limit of € 200
     */
    @Test
    public void testBuyLimitOrdersOnlyOnOtherSide_Ex10() {
        Order standingLimit = buyL(REFERENCE_PRICE, ORDER_QTY);
        Order incoming = sellMtoL(ORDER_QTY);

        ob.submit(standingLimit, NEW );
        assertEquals( 0, ob.getSellSide().getOrders().size());
        assertEquals( 1, ob.getBuySide().getOrders().size());
        assertEquals(standingLimit.getClientOrderID(), ob.getBuySide().getOrders().iterator().next().getClientOrderID());

        testMarketToLimitWithLimitOnlyOnOtherSideOK(ob, standingLimit, incoming,
                standingLimit.getClientOrderID(), incoming.getClientOrderID());
    }


    /**
     * A market-to-limit order meets an order book with limit orders only on the other side of the order book
     * Both orders are executed at the lowest ask limit of € 200.
     */
    @Test
    public void testSellLimitOrdersOnlyOnOtherSide_Ex11() {
        Order standingLimit = sellL(REFERENCE_PRICE, ORDER_QTY);
        Order incoming = buyMtoL(ORDER_QTY);

        ob.submit(standingLimit, NEW );
        assertEquals( 1, ob.getSellSide().getOrders().size());
        assertEquals( 0, ob.getBuySide().getOrders().size());
        assertEquals( standingLimit.getClientOrderID(), ob.getSellSide().getOrders().iterator().next().getClientOrderID());


        testMarketToLimitWithLimitOnlyOnOtherSideOK(ob, standingLimit, incoming,
                incoming.getClientOrderID(), standingLimit.getClientOrderID());
    }

    /**
     * A market-to-limit order meets an order book with market orders and limit orders on the other side of the order book
     * The market-to-limit order is rejected. A price is not determined and no orders are executed
     */
    @Test
    public void testBuyMarketAndLimitOnOtherSideRejection_Ex12() {
        Order standingMarket = buyM(ORDER_QTY, time("09:01:00"));
        Order standingLimit = buyL(199, ORDER_QTY, time("08:55:00"));
        assertEquals( true, ob.submit(standingMarket, NEW).isEmpty() );
        assertEquals( true, ob.submit(standingLimit, NEW).isEmpty() );

        // check standing order
        final Runnable checkOrderbook = () -> {
            assertEquals(1, ob.getBuySide().getLimitOrders().size());
            assertEquals(1, ob.getBuySide().getMarketOrders().size());
        };
        checkOrderbook.run();

        Order incoming = sellMtoL(ORDER_QTY);
        List<Trade> trades = ob.submit(incoming, NEW);
        Trade trade = checkMtoLOrderRejection(trades);
        assertEquals( incoming.getClientOrderID(), trade.getSell().getClientOrderID() );
        assertNull( trade.getBuy() );

        // check OB is unchanged
        checkOrderbook.run();
    }


    /**
     * A market-to-limit order meets an order book in which there are no orders on the other side of the order book.
     * The market-to-limit order is rejected. A price is not determined and no orders are executed.
     */
    @Test
    public void testSellMarketAndLimitOnOtherSideRejection_Ex13() {
        Runnable checkOrderbookEmpty = () -> {
            assertTrue(ob.getBuySide().getOrders().isEmpty());
            assertTrue(ob.getSellSide().getOrders().isEmpty());
        };
        checkOrderbookEmpty.run();

        Order incoming = sellMtoL(ORDER_QTY);
        List<Trade> trades = ob.submit(incoming, NEW);
        Trade trade = checkMtoLOrderRejection(trades);
        assertEquals( incoming.getClientOrderID(), trade.getSell().getClientOrderID() );
        assertNull( trade.getBuy() );

        // check OB is unchanged
        checkOrderbookEmpty.run();
    }

    //-------------------------------------------------------------------------------------------
    // Standing orders face incoming LIMIT order
    //-------------------------------------------------------------------------------------------



    //
    // test helper methods
    //


    private Trade checkMtoLOrderRejection( List<Trade> trades ) {
        assertEquals( 1, trades.size() );
        Trade trade = trades.iterator().next();
        assertEquals(ExecType.REJECTED, trade.getExecType());
        assertEquals(RejectReason.INSUFFICIENT_OB_CONSTELLATION.getMsg(), trade.getOrderRejectReason());
        assertEquals( 0, trade.getExecutionQty() );
        assertEquals(0, trade.getPrice(), TestConstants.ROUNDING_DELTA);

        return trade;
    }


    private void testMarketToLimitWithLimitOnlyOnOtherSideOK( Orderbook ob, Order standingLimit, Order incoming,
                                                              String expectedTradeBuyOrderID,
                                                              String expectedTradeSellOrderID) {
        List<Trade> trades = ob.submit(incoming, NEW);
        assertEquals( 1, trades.size() );
        Trade trade = trades.iterator().next();
        assertEquals( expectedTradeBuyOrderID, trade.getBuy().getClientOrderID());
        assertEquals( expectedTradeSellOrderID, trade.getSell().getClientOrderID() );
        assertEquals( ORDER_QTY, trade.getExecutionQty() );
        assertEquals( standingLimit.getPrice(), trade.getPrice(), TestConstants.ROUNDING_DELTA);

        // validate nothing has altered the OB
        assertEquals( 0, ob.getSellSide().getOrders().size());
        assertEquals( 0, ob.getBuySide().getOrders().size());
    }

    private void testStandingMarketAndLimitOrderSell(double standingLimitPrice, double expectedExecutionPrice) {
        Order standingMarket = sellM(ORDER_QTY, time("09:01:00"));
        Order standingLimit = sellL(standingLimitPrice, 1000, time("09:02:00"));
        ob.submit(standingMarket, NEW);
        ob.submit(standingLimit, NEW);

        Order incomingOrder = buyM(ORDER_QTY);
        List<Trade> trades = ob.submit(incomingOrder, NEW);
        assertEquals( 1, trades.size());
        assertEquals( 0, ob.getBuySide().getOrders().size() );
        assertEquals( 1, ob.getSellSide().getOrders().size());

        Trade trade = trades.get(0);
        assertEquals( expectedExecutionPrice, trade.getPrice(), TestConstants.ROUNDING_DELTA );
        assertEquals(ORDER_QTY, trade.getExecutionQty() );
        assertEquals( "The market order has precedence to be executed",
                        standingMarket.getClientOrderID(), trade.getSell().getClientOrderID() );
        assertEquals( incomingOrder.getClientOrderID(), trade.getBuy().getClientOrderID() );
    }


    private void testStandingMarketAndLimitOrderBid(double standingLimitPrice, double expectedExecutionPrice) {
        Order standingMarket = buyM(ORDER_QTY, time("09:01:00"));
        Order standingLimit = buyL(standingLimitPrice, 1000, time("09:02:00"));
        ob.submit(standingMarket, NEW);
        ob.submit(standingLimit, NEW);

        Order incomingOrder = sellM(ORDER_QTY);
        List<Trade> trades = ob.submit(incomingOrder, NEW);
        assertEquals( 1, trades.size());
        assertEquals( 1, ob.getBuySide().getOrders().size() );
        assertEquals( 0, ob.getSellSide().getOrders().size());

        Trade trade = trades.get(0);
        assertEquals( expectedExecutionPrice, trade.getPrice(), TestConstants.ROUNDING_DELTA );
        assertEquals(ORDER_QTY, trade.getExecutionQty() );
        assertEquals( "The market order has precedence to be executed",
                        standingMarket.getClientOrderID(), trade.getBuy().getClientOrderID() );
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
        assertEquals(ORDER_QTY, trade.getExecutionQty());
        assertEquals( buy.getClientOrderID(), trade.getBuy().getClientOrderID() );
        assertEquals( sell.getClientOrderID(), trade.getSell().getClientOrderID() );
        assertEquals( "incoming buy side", 0, ob.getBuySide().getOrders().size() );
        assertEquals( "standing sell side", 0, ob.getSellSide().getOrders().size() );
    }
}
