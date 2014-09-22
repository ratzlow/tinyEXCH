package net.tinyexch.order;

/**
 * Orders of both sides could be matched with a certain quantity and price. Both order originators will be informed
 * independently of this trade with an execution.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-17
 */
public final class Trade {
    private final Order buy;
    private final Order sell;

    private final double executionPrice;

    //----------- quantities----------------
    /** A round lot is composed of round lot parts or multiples thereof */
    private final int roundLots;

    /**
     * Odd lots are composed of odd lot parts (smaller than the equity-specific round lot size) and possibly
     * further round lot parts
     */
    private final int oddLots;
    //--------------------------------------


    //--------------------------------------
    // constructor
    //--------------------------------------

    public Trade( Order buy, Order sell, double executionPrice, int roundLots, int oddLots ) {
        this.buy = buy;
        this.sell = sell;
        this.executionPrice = executionPrice;
        this.roundLots = roundLots;
        this.oddLots = oddLots;
    }

    //--------------------------------------
    // accessors
    //--------------------------------------

    public Order getBuy() {
        return buy;
    }

    public Order getSell() {
        return sell;
    }

    public double getExecutionPrice() {
        return executionPrice;
    }

    public int getRoundLots() {
        return roundLots;
    }

    public int getOddLots() {
        return oddLots;
    }
}
