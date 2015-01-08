package net.tinyexch.order;

import java.time.Instant;

/**
 * Orders of both sides could be matched with a certain quantity and price. Both order originators will be informed
 * independently of this trade with an execution.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-17
 */
public final class Trade {

    private Instant executionTime = Instant.now();

    /** @link FIX:17 */
    private String execID;


    /** @link FIX:44 */
    private double price;

    /** @link FIX:32 */
    private int executionQty;

    /** @link FIX:150 */
    private ExecType execType;

    /** @link FIX:103 */
    private String orderRejectReason;

    // TODO (FRa) : (FRa) : this is very insufficient as the Trade reports look very different for both sides
    private Order buy;
    private Order sell;


    //----------- quantities----------------
    /** A round lot is composed of round lot parts or multiples thereof */
    private int roundLots;

    /**
     * Odd lots are composed of odd lot parts (smaller than the equity-specific round lot size) and possibly
     * further round lot parts
     */
    private int oddLots;
    //--------------------------------------


    //--------------------------------------
    // constructor
    //--------------------------------------

    public Trade( Order buy, Order sell, double executionPrice, int roundLots, int oddLots ) {
        this.buy = buy;
        this.sell = sell;
        this.price = executionPrice;
        this.roundLots = roundLots;
        this.oddLots = oddLots;
    }

    private Trade() {}

    public static Trade of() { return new Trade(); }

    //---------------------------------------------------
    // getters & setters
    //---------------------------------------------------

    public int getRoundLots() {
        return roundLots;
    }

    public int getOddLots() {
        return oddLots;
    }

    public Instant getExecutionTime() {
        return executionTime;
    }

    public Trade setExecutionTime(Instant executionTime) {
        this.executionTime = executionTime;
        return this;
    }

    public String getExecID() {
        return execID;
    }

    public Trade setExecID(String execID) {
        this.execID = execID;
        return this;
    }

    public double getPrice() {
        return price;
    }

    public Trade setPrice(double price) {
        this.price = price;
        return this;
    }

    public Order getBuy() {
        return buy;
    }

    public Trade setBuy(Order buy) {
        this.buy = buy;
        return this;
    }

    public Order getSell() {
        return sell;
    }

    public Trade setSell(Order sell) {
        this.sell = sell;
        return this;
    }

    public int getExecutionQty() {
        return executionQty;
    }

    public Trade setExecutionQty(int executionQty) {
        this.executionQty = executionQty;
        return this;
    }

    public ExecType getExecType() {
        return execType;
    }

    public Trade setExecType(ExecType execType) {
        this.execType = execType;
        return this;
    }

    public String getOrderRejectReason() {
        return orderRejectReason;
    }

    public Trade setOrderRejectReason(String orderRejectReason) {
        this.orderRejectReason = orderRejectReason;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Trade{");
        sb.append("executionTime=").append(executionTime);
        sb.append(", execID='").append(execID).append('\'');
        sb.append(", price=").append(price);
        sb.append(", executionQty=").append(executionQty);
        sb.append(", buy=").append(buy);
        sb.append(", sell=").append(sell);
        sb.append(", roundLots=").append(roundLots);
        sb.append(", oddLots=").append(oddLots);
        sb.append(", execType=").append(execType);
        sb.append(", orderRejectReason=").append(orderRejectReason);
        sb.append('}');
        return sb.toString();
    }
}
