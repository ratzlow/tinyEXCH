package net.tinyexch.ob.price.safeguard;

import java.time.Instant;

/**
 * If the price of an instruments exceeds certain limits defined for an
 * {@link net.tinyexch.exchange.trading.form.auction.Auction} or
 * {@link net.tinyexch.exchange.trading.form.continuous.ContinuousTrading} this event is raised to trigger appropriate
 * actions.
 * If a market maker exists for this instrument he is obliged to provide quotes.
 *
 * This class is immutable thus thread safe.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-29
 * @link chap 11
 */
public class VolatilityInterruption {
    private final Instant timestamp = Instant.now();
    private final double indicativePrice;
    private final PriceRange staticRange;
    private final PriceRange dynamicRange;

    public VolatilityInterruption(double indicativePrice, PriceRange staticRange, PriceRange dynamicRange) {
        this.indicativePrice = indicativePrice;
        this.staticRange = staticRange;
        this.dynamicRange = dynamicRange;
    }

    public double getIndicativePrice() {
        return indicativePrice;
    }

    public PriceRange getStaticRange() {
        return staticRange;
    }

    public PriceRange getDynamicRange() {
        return dynamicRange;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "VolatilityInterruption{" +
                "timestamp=" + timestamp +
                ", indicativePrice=" + indicativePrice +
                ", staticRange=" + staticRange +
                ", dynamicRange=" + dynamicRange +
                '}';
    }
}
