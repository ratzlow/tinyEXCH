package net.tinyexch.ob.price.safeguard;

import net.tinyexch.order.OrderType;

/**
 * {@link net.tinyexch.ob.price.safeguard.VolatilityInterruption} can be thrown in auctions and
 * continuous trading. As far as designated sponsors (market maker) exists the will enter quotes during
 * volatility interruptions. Executed MidPoint orders are not considered.
 *
 * In auction: fire only at end of call phase // TODO (FRa) : (FRa) : test
 *
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-09
 * @link chap 11
 */
public abstract class VolatilityInterruptionEmitter {

    //--------------------------------------------------------------------
    /**
     * Max percentage deviation symmetrically pos/neg of reference price retrieved as last price in an auction
     * on current trading day.
     */
    private final float staticPriceRangePercentage;

    /**
     * Either the last price determined in an auction or if as fallback the last traded price. (price 2). Only updated
     * on trading day if an auction was conducted. Ergo: price remains largely unchanged during trading.
     */
    private double staticPriceRangeRefPrice;

    //--------------------------------------------------------------------

    /**
     * Similar to #staticPriceRangePercentage but orientates on last traded price in auction or continuous trading.
     */
    private final float dynamicPriceRangePercentage;

    /**
     * Initialized as "last traded price" in auction or continuous trading.
     * Later on readjusted after order was matched in continuous trading.
     */
    private double dynamicPriceRangeRefPrice;
    //--------------------------------------------------------------------

    protected VolatilityInterruptionEmitter(float staticPriceRangePercentage, float dynamicPriceRangePercentage) {
        this.staticPriceRangePercentage = staticPriceRangePercentage;
        this.dynamicPriceRangePercentage = dynamicPriceRangePercentage;
    }

    //--------------------------------------------------------------------
    // public API
    //--------------------------------------------------------------------

    public void updateStaticRefPrice(double staticPriceRangeRefPrice) {
        this.staticPriceRangeRefPrice = staticPriceRangeRefPrice;
    }

    public void updateDynamicRefPrice( double dynamicPriceRangeRefPrice ) {
        this.dynamicPriceRangeRefPrice = dynamicPriceRangeRefPrice;
    }

    public void validateIndicativePrice( double indicativePrice, OrderType matchedOrderType ) {
        if ( !isIndicativePriceOutsideStaticPriceRange(indicativePrice) ||
             !isIndicativePriceOutsideDynamicPriceRange(indicativePrice)) {
            fireVolatilityInterruption();
        }
    }

    protected abstract void fireVolatilityInterruption();

    //--------------------------------------------------------------------
    // public API
    //--------------------------------------------------------------------

    // indicative price
    // dynamic price range
    // static price range
    // reference price -> in contTrad: after order was matched
    // last traded price
    // executions of MidPoint orders are to be ignored
    private boolean isIndicativePriceOutsideDynamicPriceRange( double indicativePrice ) {
        return isInPriceRange( indicativePrice, dynamicPriceRangeRefPrice, dynamicPriceRangePercentage);
    }

    private boolean isIndicativePriceOutsideStaticPriceRange( double indicativePrice ) {
        return isInPriceRange( indicativePrice, staticPriceRangeRefPrice, staticPriceRangePercentage );
    }

    // TODO (FRa) : (FRa) : add test to avoid neg bounds
    private boolean isInPriceRange(double indicativePrice, double refPrice, float priceRange) {
        double lowerPrice = refPrice * (100-priceRange/2) / 100;
        double upperPrice = refPrice * (100+priceRange/2) / 100;
        return (lowerPrice <= indicativePrice && indicativePrice <= upperPrice);
    }
}
