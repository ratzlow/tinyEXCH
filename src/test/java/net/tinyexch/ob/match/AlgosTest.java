package net.tinyexch.ob.match;

import net.tinyexch.exchange.trading.form.auction.DefaultPriceDeterminationPhase;
import net.tinyexch.ob.Orderbook;
import net.tinyexch.order.Order;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoublePredicate;
import java.util.stream.Collectors;

import static net.tinyexch.ob.TestConstants.ROUNDING_DELTA;
import static net.tinyexch.ob.match.Algos.*;
import static net.tinyexch.ob.match.OrderFactory.newOrder;
import static net.tinyexch.order.OrderType.LIMIT;
import static net.tinyexch.order.Side.BUY;
import static net.tinyexch.order.Side.SELL;

/**
 * Test functional wise if the algos work.
 *
 * @author ratzlow@gmail.com
 * @since 2014-10-19
 */
public class AlgosTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlgosTest.class);

    @Test
    public void testSearchClosestValue() {
        DoublePredicate withinBoundaries = p -> true;
        Assert.assertEquals(198, searchClosest(197, new double[]{202.0, 201.0, 200.0, 198.0, 191.0, 179.0}, withinBoundaries).get(), ROUNDING_DELTA);
        Assert.assertEquals(3.1, searchClosest(3.0, new double[]{2.0, 2.5, 3.1, 4.8, 6.0, 6.9}, withinBoundaries).get(), ROUNDING_DELTA);
        Assert.assertEquals(2.7, searchClosest(1.0, new double[]{2.7}, withinBoundaries).get(), ROUNDING_DELTA);
        Assert.assertEquals(1.5, searchClosest(1.5, new double[]{1.5, 2.2}, withinBoundaries).get(), ROUNDING_DELTA);
        Assert.assertEquals(2.2, searchClosest(2.0, new double[]{1.5, 2.2, 2.2, 2.2, 5.7}, withinBoundaries).get(), ROUNDING_DELTA);
        Assert.assertEquals(2.2, searchClosest(2.0, new double[]{1.5, 2.2, 2.2, 2.2, 5.7}, withinBoundaries).get(), ROUNDING_DELTA);
        Assert.assertEquals("There are 2 applicable values, each same distance off the search value",
                1.5, searchClosest(2.0, new double[] {1.5, 1.5, 2.5, 2.5, 5.7}, withinBoundaries).get(), ROUNDING_DELTA);
        Assert.assertEquals(5.7, searchClosest(5.1, new double[]{1.5, 1.5, 2.5, 2.5, 5.7}, withinBoundaries).get(), ROUNDING_DELTA);
        Assert.assertEquals(1, searchClosest(2, new double[]{1, 1, 1, 1}, withinBoundaries).get(), ROUNDING_DELTA);
        Assert.assertEquals(1, searchClosest(2, new double[]{1, 3, 3, 3, 3, 7}, withinBoundaries).get(), ROUNDING_DELTA);
        Assert.assertEquals(3, searchClosest(5, new double[]{1, 3, 3, 3, 3, 7}, withinBoundaries).get(), ROUNDING_DELTA);
        Assert.assertEquals(7, searchClosest(6, new double[]{1, 3, 3, 3, 3, 7}, withinBoundaries).get(), ROUNDING_DELTA);
    }


    @Test
    public void testSearchClosestPriceOnOufOfPriceRange() {
        Assert.assertEquals( Optional.empty(), searchClosestBid( 201, new double[]{200, 199} ) );
        Assert.assertEquals( Optional.empty(), searchClosestAsk( 195, new double[]{198, 199} ) );
    }


    @Test
    public void testOrderbookBuilding() {

        Orderbook ob = new Orderbook(
            new Order[]{ newOrder(BUY, 202, 200, LIMIT), newOrder(BUY, 201, 200, LIMIT), newOrder(BUY, 200, 200, LIMIT)},
            new Order[]{ newOrder(SELL, 200, 100, LIMIT), newOrder(SELL, 198, 200, LIMIT), newOrder(SELL, 197, 400, LIMIT)}
        );

        List<Order> orderedBuys = ob.getBuySide().getBest(DefaultPriceDeterminationPhase.BUY_PRICE_ORDERING);
        List<Order> orderedSells = ob.getSellSide().getBest(DefaultPriceDeterminationPhase.SELL_PRICE_ORDERING);

        Assert.assertArrayEquals(new Double[]{202D, 201D, 200D},
                orderedBuys.stream().map(Order::getPrice).collect(Collectors.toList()).toArray());
        Assert.assertArrayEquals(new Double[]{197D, 198D, 200D},
                orderedSells.stream().map(Order::getPrice).collect(Collectors.toList()).toArray());

        double[] bidPrices = orderedBuys.stream().mapToDouble(Order::getPrice).toArray();
        double[] askPrices = orderedSells.stream().mapToDouble(Order::getPrice).toArray();

        double worstMatchableAskPrice = searchClosestAsk(orderedBuys.get(0).getPrice(), askPrices).get();
        double worstMatchableBidPrice = searchClosestBid(orderedSells.get(0).getPrice(), bidPrices).get();

        Assert.assertEquals(200, worstMatchableBidPrice, ROUNDING_DELTA);
        Assert.assertEquals(200, worstMatchableAskPrice, ROUNDING_DELTA);
    }


    @Deprecated // TODO (FRa) : (FRa) : use proper impl
    double binarySearchClosest( double bestPrice, double[] prices,  boolean preferLowerPrice ) {
        Objects.requireNonNull(prices, "Prices must not be empty!");
        LOGGER.debug("bestPrice={} inputPrices={}", bestPrice, Arrays.toString(prices));
        if (prices.length == 1) return prices[0];

        // deviation from bestPrice increases
        boolean evenElems = prices.length % 2 == 0;
        int halfLength = prices.length / 2;

        int leftLowerIdx = 0;
        int leftUpperIdx = evenElems ? halfLength - 1 : halfLength;
        int rightLowerIdx = evenElems ? halfLength : halfLength + 1;
        int rightUpperIdx = prices.length;

        double distanceLeft  = Math.abs(bestPrice - prices[leftUpperIdx]);
        double distanceRight = Math.abs(bestPrice - prices[rightLowerIdx]);

        LOGGER.debug("1.) distanceLeft={} distanceRight={}", distanceLeft, distanceRight);

        final double closestPrice;

        // left direction
        if ( distanceLeft < distanceRight ) {
            LOGGER.debug("2.) <-- [{}]-[{}]", leftLowerIdx, leftUpperIdx);
            closestPrice = binarySearchClosest(bestPrice, Arrays.copyOfRange(prices, leftLowerIdx, leftUpperIdx+1), preferLowerPrice );

            // right direction
        } else if (distanceLeft > distanceRight) {
            LOGGER.debug("3.) --> [{}]-[{}]", rightLowerIdx, rightUpperIdx);
            closestPrice = binarySearchClosest(bestPrice, Arrays.copyOfRange(prices, rightLowerIdx, rightUpperIdx), preferLowerPrice);

            // 2 mid numbers are same
        } else {
            LOGGER.debug("4.) --> leftUpperPrice={} rightLowerPrice={}", prices[leftUpperIdx], prices[rightLowerIdx]);

            if (preferLowerPrice) {
                closestPrice = binarySearchClosest(bestPrice, Arrays.copyOfRange(prices, leftLowerIdx, leftUpperIdx+1), preferLowerPrice );
            } else {
                closestPrice = binarySearchClosest(bestPrice, Arrays.copyOfRange(prices, rightLowerIdx, rightUpperIdx), preferLowerPrice);
            }
        }

        return closestPrice;
    }
}
