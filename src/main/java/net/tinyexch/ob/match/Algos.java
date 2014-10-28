package net.tinyexch.ob.match;

import net.tinyexch.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;

/**
 * For certain tasks sort and search algorithms are used. They implementation and exposure is kept here. This class is
 * stateless and thread safe. As it is just a collection of methods.
 *
 * @author ratzlow@gmail.com
 * @since 2014-10-19
 */
public final class Algos {
    private static final Logger LOGGER = LoggerFactory.getLogger(Algos.class);

    /** Hide the constructor */
    private Algos(){}

    //------------------------------------------------------------------------------------------------
    // public API
    //------------------------------------------------------------------------------------------------

    /**
     * Derive the quantity on #orders that could be matched in accordance to the given #priceFilter.
     *
     * @param orders list of orders we want to retrieve the accumulated quantity that could be matched
     * @param priceFilter defines the price corridor for matching
     * @return number of shares that could be matched
     */
    public static int getMatchableQuantity(List<Order> orders, Predicate<? super Order> priceFilter) {
        return orders.stream().filter( priceFilter ).mapToInt(Order::getOrderQty).sum();
    }

    /**
     * Find the bid price deepest in the sorted prices that is closest to the ask price (top of the book) so we know
     * down to what bid price the bids could be matched. Relates to the "worst" matchable bid price that is still in the
     * market.
     *
     * @param askSearchPrice best ask price retrieved from the top of the ask side
     * @param bidPrices ordered bid prices with best bid (highest) coming first
     * @return worst ask price still in the market
     */
    public static double searchClosestBid(double askSearchPrice, double[] bidPrices ) {
        return searchClosest(askSearchPrice, bidPrices, bid -> bid >= askSearchPrice);
    }

    /**
     * Same as #searchClosestBid(double, double[]) but with opposite semantics
     */
    public static double searchClosestAsk(double bidSearchPrice, double[] askPrices ) {
        return searchClosest(bidSearchPrice, askPrices, ask -> ask <= bidSearchPrice);
    }

    /**
     * @param bestPrice top of the book price from other side
     * @param prices this side prices to search through
     * @param withinBoundaries specifies what price is within a valid price range
     * @return closest price from #prices to #bestPrice but #withinBoundaries
     */
    static double searchClosest(double bestPrice, double[] prices, DoublePredicate withinBoundaries ) {
        // TODO (FRa) : (FRa) : replace with equivalent binSearch
        return sequentialSearchClosest( bestPrice, prices, Math::min, withinBoundaries );
    }

    //--------------------------------------------------------------------------------------------------
    // hide the algos we effectively use
    //--------------------------------------------------------------------------------------------------

    /**
     * Linear search algo over sorted #prices stopping as soon the price found is worse than the one found previously.
     * Not very smart!
     *
     * @param searchPrice
     * @param prices
     * @param sameDistanceResolver what to do if the #searchPrice is exactly in the middle of two prices.
     *                             Prefer higher or lower price?
     * @param withinBoundaries
     * @return
     */
    private static double sequentialSearchClosest( final double searchPrice, double[] prices,
                                    ToDoubleBiFunction<Double, Double> sameDistanceResolver,
                                    DoublePredicate withinBoundaries ) {

        Objects.requireNonNull(prices, "Prices must not be empty!");
        LOGGER.debug("bestPrice={} inputPrices={}", searchPrice, Arrays.toString(prices));
        if (prices.length == 1) return prices[0];

        double closestPrice = prices[0];
        double minDistance = Math.abs(searchPrice - closestPrice);

        boolean distanceIncreased = false;
        int i=0;
        do {
            double price = prices[i];
            double distance = Math.abs( price - searchPrice );

            if ( distance < minDistance && withinBoundaries.test(price) ) {
                closestPrice = price;
                minDistance = distance;

            } else if ( distance == minDistance ) {
                closestPrice = sameDistanceResolver.applyAsDouble( price, closestPrice );

            } else {
                distanceIncreased = true;
            }

            i++;
            // stop as soon distance to searched price increased
        } while ( i < prices.length && !distanceIncreased);

        LOGGER.debug("Stopped evaluation at prices[{}]={}", i-1, prices[i-1]);
        return closestPrice;
    }
}
