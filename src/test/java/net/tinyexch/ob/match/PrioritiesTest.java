package net.tinyexch.ob.match;

import net.tinyexch.order.Order;
import net.tinyexch.order.OrderType;
import net.tinyexch.order.Side;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static net.tinyexch.ob.match.OrderFactory.*;

/**
 * Check combinations of orders and their sorting. Market orders be on top if by price check.
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-16
 */
public class PrioritiesTest {

    @Test
    public void testOlderTimestampComesFirst() {
        List<Order> someOrders = createUnsortedOrders(Side.BUY);
        Collections.shuffle(someOrders);
        someOrders.sort(Priorities.TIME);
        Order oldestOrder = someOrders.get(0);
        Order middle = someOrders.get(1);
        Order newest = someOrders.get(2);

        Assert.assertTrue(oldestOrder.getTimestamp().isBefore(middle.getTimestamp()));
        Assert.assertTrue(middle.getTimestamp().isBefore(newest.getTimestamp()));
    }

    @Test
    public void testBuyFirstMarketThanHighestPrice() {
        List<Order> buys = createUnsortedOrders(Side.BUY);
        // sort them
        buys.sort(Priorities.PRICE.reversed());
        List<String> sortedBuyIDs = buys.stream().map(Order::getClientOrderID).collect(toList());
        Assert.assertEquals("Buy side: MKT first, than highest limit, than lower limit",
                Arrays.asList("3", "2", "1"), sortedBuyIDs);
    }

    @Test
    public void testSellFirstMarketThanLowestPrice() {
        List<Order> sells = createUnsortedOrders(Side.SELL);
        sells.sort(Priorities.PRICE);
        List<String> sortedSellIDs = sells.stream().map(Order::getClientOrderID).collect(toList());
        Assert.assertEquals("Sell side: MKT first, than lowest limit, than higher limit",
                Arrays.asList("3", "1", "2"), sortedSellIDs);
    }

    @Test()
    public void testPriceTimePrioOnPrioQueue() {
        Order o_1 = sellL(201, 5000, time("09:10:40")).setSubmitSequence(3);
        Order o_2 = sellL(201, 8000, time("09:10:40")).setSubmitSequence(2);
        Order o_3 = sellL(201, 2000, time("09:13:13")).setSubmitSequence(4);
        Order o_4 = sellL(203, 500,  time("08:55:00")).setSubmitSequence(1);
        Comparator<Order> comparator = MatchEngine.SELL_PRICE_TIME_ORDERING.thenComparing(Priorities.SUBMIT_SEQUENCE);
        PriorityQueue<Order> queue = new PriorityQueue<>(comparator);
        queue.addAll( Arrays.asList(o_1, o_2, o_3, o_4) );
        Assert.assertEquals( queue.poll().getClientOrderID(), o_2.getClientOrderID());
        Assert.assertEquals( queue.poll().getClientOrderID(), o_1.getClientOrderID());
        Assert.assertEquals( queue.poll().getClientOrderID(), o_3.getClientOrderID());
        Assert.assertEquals( queue.poll().getClientOrderID(), o_4.getClientOrderID());
    }


    private List<Order> createUnsortedOrders(Side side) {
        Order o1 = Order.of("1", side).setPrice(87).setOrderType(OrderType.LIMIT).setOrderQty(18)
                .setTimestamp(newTS(1));
        Order o2 = Order.of("2", side).setPrice(89).setOrderType(OrderType.LIMIT).setOrderQty(20)
                .setTimestamp(newTS(2));
        Order o3 = Order.of("3", side).setOrderType(OrderType.MARKET).setOrderQty(70)
                .setTimestamp(newTS(3));
        return Arrays.asList(o1, o2, o3);
    }

    private Instant newTS(int secOffset) {
        return LocalDateTime.now().plusSeconds(secOffset).toInstant(ZoneOffset.UTC);
    }
}
