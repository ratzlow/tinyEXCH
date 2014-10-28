package net.tinyexch.ob.match;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.DoublePredicate;

import static net.tinyexch.ob.TestConstants.ROUNDING_DELTA;
import static net.tinyexch.ob.match.Algos.searchClosest;

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
        Assert.assertEquals(198, searchClosest(197, new double[]{202.0, 201.0, 200.0, 198.0, 191.0, 179.0}, withinBoundaries), ROUNDING_DELTA);
        Assert.assertEquals(3.1, searchClosest(3.0, new double[]{2.0, 2.5, 3.1, 4.8, 6.0, 6.9}, withinBoundaries), ROUNDING_DELTA);
        Assert.assertEquals(2.7, searchClosest(1.0, new double[]{2.7}, withinBoundaries), ROUNDING_DELTA);
        Assert.assertEquals(1.5, searchClosest(1.5, new double[]{1.5, 2.2}, withinBoundaries), ROUNDING_DELTA);
        Assert.assertEquals(2.2, searchClosest(2.0, new double[]{1.5, 2.2, 2.2, 2.2, 5.7}, withinBoundaries), ROUNDING_DELTA);
        Assert.assertEquals(2.2, searchClosest(2.0, new double[]{1.5, 2.2, 2.2, 2.2, 5.7}, withinBoundaries), ROUNDING_DELTA);
        Assert.assertEquals("There are 2 applicable values, each same distance off the search value",
                1.5, searchClosest(2.0, new double[] {1.5, 1.5, 2.5, 2.5, 5.7}, withinBoundaries), ROUNDING_DELTA);
        Assert.assertEquals(5.7, searchClosest(5.1, new double[]{1.5, 1.5, 2.5, 2.5, 5.7}, withinBoundaries), ROUNDING_DELTA);
        Assert.assertEquals(1, searchClosest(2, new double[]{1, 1, 1, 1}, withinBoundaries), ROUNDING_DELTA);
        Assert.assertEquals(1, searchClosest(2, new double[]{1, 3, 3, 3, 3, 7}, withinBoundaries), ROUNDING_DELTA);
        Assert.assertEquals(3, searchClosest(5, new double[]{1, 3, 3, 3, 3, 7}, withinBoundaries), ROUNDING_DELTA);
        Assert.assertEquals(7, searchClosest(6, new double[]{1, 3, 3, 3, 3, 7}, withinBoundaries), ROUNDING_DELTA);
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
