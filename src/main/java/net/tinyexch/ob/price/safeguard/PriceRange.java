package net.tinyexch.ob.price.safeguard;

/**
 * Describes a price range around a reference price and a deviation in the neg and pos direction. This class is
 * thread safe.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-13
 */
public class PriceRange {
    private final double lower;
    private final double upper;

    private final double refPrice;
    private final float priceDeviationPerc;

    /**
     *
     * @param refPrice base price
     * @param priceDeviationPerc percent range around base price to define upper & lower boundary
     */
    PriceRange(double refPrice, float priceDeviationPerc) {
        this.lower = lowerBound(refPrice, priceDeviationPerc);
        this.upper = upperBound(refPrice, priceDeviationPerc);
        this.refPrice = refPrice;
        this.priceDeviationPerc = priceDeviationPerc;
    }

    /**
     * @param other to compare the upper and lower bound against current instance
     * @return true ... ranges are intersecting
     */
    boolean intersect( PriceRange other ) {
        return !(this.lower > other.upper || this.upper < other.lower);
    }

    public float getPriceDeviationPerc() { return priceDeviationPerc; }

    boolean contains(double price) { return lower <= price && price <= upper; }

    private double lowerBound(double refPrice, float deviation) { return refPrice * (100-deviation) / 100; }
    private double upperBound(double refPrice, float deviation) { return refPrice * (100+deviation) / 100; }

    @Override
    public String toString() {
        return "PriceRange{" +
                "lower=" + lower +
                ", upper=" + upper +
                ", refPrice=" + refPrice +
                ", priceDeviationPerc=" + priceDeviationPerc +
                '}';
    }
}
