package net.tinyexch.ob.price.safeguard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * {@link net.tinyexch.ob.price.safeguard.VolatilityInterruption} can be thrown in auctions and
 * continuous trading. As far as designated sponsors (market maker) exists the will enter quotes during
 * volatility interruptions. Executed MidPoint orders are not considered.
 *
 * This class is not thread safe and needs client synchronization when updating deviations or ref prices. It will
 * accept negative prices as price determination component has to ensure valid prices in general.
 *
 * In auction: fire only at end of call phase // TODO (FRa) : (FRa) : test
 *
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-09
 * @link chap 11
 */
public abstract class VolatilityInterruptionEmitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VolatilityInterruptionEmitter.class);

    public static final VolatilityInterruptionEmitter NO_OP_EMITTER = new VolatilityInterruptionEmitter( 0,0,0,0 ) {
        @Override protected void fireVolatilityInterruption() { }
    };

    /**
     * Deviation ... max percentage deviation symmetrically pos/neg of reference price retrieved as last price in an auction
     * on current trading day.
     *
     * Static reference price ... either the last price determined in an auction or if as fallback the last traded price. (price 2). Only updated
     * on trading day if an auction was conducted. Ergo: price remains largely unchanged during trading.
     */
    private PriceRange staticRange;


    /**
     * Deviation ... similar to #staticPriceDeviationPerc but orientates on last traded price in auction or continuous trading.
     *
     * Dyn reference price ... Initialized as "last traded price" in auction or continuous trading.
     * Later on readjusted after order was matched in continuous trading.
     */
    private PriceRange dynamicRange;

    //--------------------------------------------------------------------

    /**
     * This listener should be invoked on any changes to the reference price to check if the new reference price is
     * within the specified range parameters.
     *
     * @param staticPriceRangeRefPrice base price that is usually a defines the midpoint stable broader range
     * @param staticPriceDeviationPerc percent by which #staticPriceRangeRefPrice differ in pos + neg direction
     * @param dynamicPriceRangeRefPrice base price changing during (cont) trading usually extending the more stable
     *                                  #staticPriceRangeRefPrice
     * @param dynamicPriceDeviationPerc defines the pos/neg deviation around #dynamicPriceRangeRefPrice
     */
    protected VolatilityInterruptionEmitter( double staticPriceRangeRefPrice, float staticPriceDeviationPerc,
                                             double dynamicPriceRangeRefPrice, float dynamicPriceDeviationPerc ) {

        this.staticRange = new PriceRange(staticPriceRangeRefPrice, staticPriceDeviationPerc);
        this.dynamicRange = new PriceRange(dynamicPriceRangeRefPrice, dynamicPriceDeviationPerc);

        if ( !staticRange.intersect(dynamicRange) )
            throw new InvalidReferencePriceException("Price ranges are not intersecting!");
    }

    //--------------------------------------------------------------------
    // public API
    //--------------------------------------------------------------------

    public void updateStaticRefPrice(double newStatRefPrice ) {
        update( newStatRefPrice, newRange -> staticRange = newRange, dynamicRange, staticRange );
    }

    public void updateDynamicRefPrice( double newDynRefPrice ) {
        update( newDynRefPrice, newRange -> dynamicRange = newRange, staticRange, dynamicRange);
    }

    private void update( double newRefPrice, Consumer<PriceRange> rangeSetter,
                         PriceRange unchangedRange, PriceRange toBeReplaced ) {
        PriceRange newRefPriceRange = new PriceRange(newRefPrice, toBeReplaced.getPriceDeviationPerc() );
        if ( newRefPriceRange.intersect(unchangedRange) ) {
            rangeSetter.accept(newRefPriceRange);
            LOGGER.debug("Updated price range to {}", newRefPriceRange.toString() );
        } else {
            String msg = String.format("New ref price range %s does not intersect with price range %s",
                    newRefPriceRange.toString(), unchangedRange.toString());
            throw new InvalidReferencePriceException( msg );
        }
    }

    public boolean checkIndicativePrice(double indicativePrice) {
        boolean valid = !staticRange.contains(indicativePrice) && !dynamicRange.contains(indicativePrice);
        if (valid) {
            fireVolatilityInterruption();
        }
        return valid;
    }

    protected abstract void fireVolatilityInterruption();
}
