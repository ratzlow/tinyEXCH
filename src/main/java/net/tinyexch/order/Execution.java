package net.tinyexch.order;


import java.time.Instant;

/**
 * Result of a (partial) match of orders of opposite sides.
 *
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-10
 */
public class Execution {
    private Instant executionTime = Instant.now();

    /** @link FIX:17 */
    private String execID;


    /** @link FIX:44 */
    private double price;

    /** @link FIX:32 */
    private int executionQty;

    // TODO (FRa) : (FRa) : this is very insufficient as the Execution reports look very different for both sides
    private Order buy;
    private Order sell;

    public static Execution of() { return new Execution(); }

    //---------------------------------------------------
    // getters & setters
    //---------------------------------------------------

    public Instant getExecutionTime() {
        return executionTime;
    }

    public Execution setExecutionTime(Instant executionTime) {
        this.executionTime = executionTime;
        return this;
    }

    public String getExecID() {
        return execID;
    }

    public Execution setExecID(String execID) {
        this.execID = execID;
        return this;
    }

    public double getPrice() {
        return price;
    }

    public Execution setPrice(double price) {
        this.price = price;
        return this;
    }

    public Order getBuy() {
        return buy;
    }

    public Execution setBuy(Order buy) {
        this.buy = buy;
        return this;
    }

    public Order getSell() {
        return sell;
    }

    public Execution setSell(Order sell) {
        this.sell = sell;
        return this;
    }

    public int getExecutionQty() {
        return executionQty;
    }

    public Execution setExecutionQty(int executionQty) {
        this.executionQty = executionQty;
        return this;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Execution{");
        sb.append("executionTime=").append(executionTime);
        sb.append(", execID='").append(execID).append('\'');
        sb.append(", price=").append(price);
        sb.append(", executionQty=").append(executionQty);
        sb.append(", buy=").append(buy);
        sb.append(", sell=").append(sell);
        sb.append('}');
        return sb.toString();
    }
}
