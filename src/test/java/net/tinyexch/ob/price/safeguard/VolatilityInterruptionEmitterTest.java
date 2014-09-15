package net.tinyexch.ob.price.safeguard;

import net.tinyexch.order.OrderType;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Check along boundary conditions at incoming prices. Doesn't check it's proper usage.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-11
 */
public class VolatilityInterruptionEmitterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(VolatilityInterruptionEmitterTest.class);

    private enum Range { IN, OUT }

    @Test
    public void testIndicativePriceInPriceRanges() throws InterruptedException {
        TestEmitter emitter = new TestEmitter(10, 20, 12, 10);
        emitter.assertRange(13.21, Range.OUT, "Price exceeds upper bound dyn range 12 + 1.2 = 13.2");
        emitter.assertRange(13.3, Range.OUT, "Price exceeds upper bound dyn range 12 + 1.2 = 13.2");
        emitter.assertRange(7.9, Range.OUT, "Price is below lower bound stat range 10 - 2 = 8");

        emitter.assertRange(13.19, Range.IN, "Below upper bound at dyn range 13.2");
        emitter.assertRange(8.1, Range.IN, "Above lower bound at statRefPrice=10");

        emitter.assertRange(13.2, Range.IN, "Exactly at upper boundary");
        emitter.assertRange(8, Range.IN, "Exactly at lower boundary");
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
        TestEmitter emitter = new TestEmitter(10, 20, 12, 10);

        // new dyn ref price on upper bound
        emitter.updateDynamicRefPrice( 11.9 );

        // new dyn ref price on lower bound
        emitter.updateDynamicRefPrice( 7.5 );

        // new stat ref price as upper bound
        emitter.updateStaticRefPrice( 9.787 );
    }



    @Test
    public void testEmitterRefPriceUpdatesFail() {
        final TestEmitter emitter = new TestEmitter(10, 20, 12, 10);
        // dyn price now over upper stat price -> fail
        Assert.assertFalse( execute(() -> emitter.updateDynamicRefPrice(15) ));
        // dyn again good and intersecting with stat price
        Assert.assertTrue(  execute(() -> emitter.updateDynamicRefPrice(12.5)));
        // dyn price now below stat price -> fail
        Assert.assertFalse(execute(() -> emitter.updateDynamicRefPrice(6.95)));

        // => last successful update is dyn ref price von 12.5
        // stat price over dyn price
        Assert.assertFalse( execute(() -> emitter.updateStaticRefPrice(20) ));
        // stat price intersecting with dyn price
        Assert.assertTrue(  execute(() -> emitter.updateStaticRefPrice(14.8)));
        // stat price below dyn price
        Assert.assertFalse(execute(() -> emitter.updateStaticRefPrice(9.05)));
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

    static class TestEmitter extends VolatilityInterruptionEmitter {
        final AtomicReference<Range> fired = new AtomicReference<>(Range.IN);

        TestEmitter(double staticPriceRangeRefPrice, float staticPriceDeviationPerc,
                    double dynamicPriceRangeRefPrice, float dynamicPriceDeviationPerc) {
            super(staticPriceRangeRefPrice, staticPriceDeviationPerc, dynamicPriceRangeRefPrice, dynamicPriceDeviationPerc);
        }

        @Override
        protected void fireVolatilityInterruption() { fired.set(Range.OUT); }

        void assertRange(double indicativePrice, Range inOrOut, String msg)
                throws InterruptedException {
            fired.set(Range.IN);
            validateIndicativePrice(indicativePrice, OrderType.MARKET);
            Assert.assertEquals(msg, inOrOut, fired.get());
        }
    }
}
