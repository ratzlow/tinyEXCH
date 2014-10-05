package net.tinyexch.ob.price.safeguard;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Check along boundary conditions at incoming prices. Doesn't check it's proper usage.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-11
 */
public class VolatilityInterruptionPriceRangeTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(VolatilityInterruptionPriceRangeTest.class);

    private enum Range { IN, OUT }

    @Test
    public void testIndicativePriceInPriceRanges() throws InterruptedException {
        TestGuard guard = new TestGuard(10, 20, 12, 10);
        guard.assertRange(13.21, Range.OUT, "Price exceeds upper bound dyn range 12 + 1.2 = 13.2");
        guard.assertRange(13.3, Range.OUT, "Price exceeds upper bound dyn range 12 + 1.2 = 13.2");
        guard.assertRange(7.9, Range.OUT, "Price is below lower bound stat range 10 - 2 = 8");

        guard.assertRange(13.19, Range.IN, "Below upper bound at dyn range 13.2");
        guard.assertRange(8.1, Range.IN, "Above lower bound at statRefPrice=10");

        guard.assertRange(13.2, Range.IN, "Exactly at upper boundary");
        guard.assertRange(8, Range.IN, "Exactly at lower boundary");
    }

    @Test
    public void testPriceRangeIntersection() {
        // first range is completely below second
        testIntersection( range(8,10), range(10,10), false );

        // first range is completely above second
        testIntersection( range(10,10), range(8,10), false );

        // second range spans first range
        testIntersection(range(10, 20), range(10, 30), true);

        // second range extends upper limit of first range
        testIntersection( range(10, 20), range(12, 10), true);
        testIntersection( range(7.5, 10), range(10, 20), true);

        // second range extends lower limit of first range
        testIntersection( range(10, 10), range(8, 25), true);

        // second range completely within first range
        testIntersection( range(10, 20), range(10, 10), true);
    }

    @Test
    public void testEmitterRefPriceUpdatesOK() {
        // price ranges are intersecting
        TestGuard guard = new TestGuard(10, 20, 12, 10);

        // new dyn ref price on upper bound
        guard.updateDynamicRefPrice(11.9);

        // new dyn ref price on lower bound
        guard.updateDynamicRefPrice(7.5);

        // new stat ref price as upper bound
        guard.updateStaticRefPrice(9.787);
    }

    @Test
    public void testEmitterRefPriceUpdatesFail() {
        final TestGuard guard = new TestGuard(10, 20, 12, 10);
        // dyn price now over upper stat price -> fail
        Assert.assertFalse( execute(() -> guard.updateDynamicRefPrice(15) ));
        // dyn again good and intersecting with stat price
        Assert.assertTrue( execute(() -> guard.updateDynamicRefPrice(12.5)));
        // dyn price now below stat price -> fail
        Assert.assertFalse( execute(() -> guard.updateDynamicRefPrice(6.95)));

        // => last successful update is dyn ref price von 12.5
        // stat price over dyn price
        Assert.assertFalse( execute(() -> guard.updateStaticRefPrice(20) ));
        // stat price intersecting with dyn price
        Assert.assertTrue( execute(() -> guard.updateStaticRefPrice(14.8)));
        // stat price below dyn price
        Assert.assertFalse( execute(() -> guard.updateStaticRefPrice(9.05)));
    }


    private boolean execute(Runnable callback) {
        boolean result = false;
        try {
            callback.run();
            result = true;
        } catch (Exception e) { LOGGER.debug("Call failed with", e ); }

        return result;
    }


    private void testIntersection(PriceRange one, PriceRange other, boolean intersectionExpected) {
        LOGGER.info("one: " + one + " other: " + other);
        Assert.assertEquals(intersectionExpected, one.intersect(other) );
    }

    private PriceRange range(double refPrice, float deviation) { return new PriceRange(refPrice, deviation); }

    static class TestGuard extends VolatilityInterruptionGuard {
        final AtomicReference<Range> fired = new AtomicReference<>(Range.IN);

        TestGuard(double staticPriceRangeRefPrice, float staticPriceDeviationPerc,
                  double dynamicPriceRangeRefPrice, float dynamicPriceDeviationPerc) {
            super(staticPriceRangeRefPrice, staticPriceDeviationPerc, dynamicPriceRangeRefPrice, dynamicPriceDeviationPerc);
        }

        @Override
        public Optional<VolatilityInterruption> checkIndicativePrice(double indicativePrice) {
            Optional<VolatilityInterruption> interruption = super.checkIndicativePrice(indicativePrice);
            interruption.ifPresent( event -> fired.set(Range.OUT) );
            return interruption;
        }

        void assertRange(double indicativePrice, Range inOrOut, String msg)
                throws InterruptedException {
            fired.set(Range.IN);
            Optional<VolatilityInterruption> event = checkIndicativePrice(indicativePrice);
            Assert.assertEquals(msg + " " + event, inOrOut, fired.get());
        }
    }
}
