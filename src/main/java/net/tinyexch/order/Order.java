package net.tinyexch.order;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Order to be matched against the other side of the orderbook. For more information regarding the FIX semantic
 *
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-27
 * @link http://www.onixs.biz/fix-dictionary
 */
// TODO (FRa) : (FRa) : provide immutable clone() result
public class Order {

    private Instant timestamp = Instant.now();

    /** @link FIX:11 */
    private final String clientOrderID;

    /** @link FIX:54 */
    private Side side;

    /** @link FIX:38 */
    private int orderQty;

    /** @link FIX:14 */
    private int cumQty;


    /**
     * @link FIX:432:
     * Conditionally required if TimeInForce <59> = GTD and ExpireTime <126> is not specified.
     */
    private LocalDateTime expirationDate;

    /**
     * // TODO (FRa) : (FRa) : check what is a sensible default
     */
    private TimeInForce timeInForce = TimeInForce.DAY;


    /** @link FIX:40 */
    private OrderType orderType;

    /** @link FIX:44 */
    private double price;


    //---------------------------------------------------------
    // constructors
    //---------------------------------------------------------

    public Order( String clientOrderID) {
        this.clientOrderID = clientOrderID;
    }

    public Order( String clientOrderID, Side side ) {
        this(clientOrderID);
        this.side = side;
    }

    /** copy constructor */
    private Order(Instant timestamp, String clientOrderID, Side side, int orderQty, int cumQty,
                 LocalDateTime expirationDate, TimeInForce timeInForce, OrderType orderType, double price) {
        this.timestamp = timestamp;
        this.clientOrderID = clientOrderID;
        this.side = side;
        this.orderQty = orderQty;
        this.cumQty = cumQty;
        this.expirationDate = expirationDate;
        this.timeInForce = timeInForce;
        this.orderType = orderType;
        this.price = price;
    }

    //---------------------------------------------------------
    // accessors
    //---------------------------------------------------------

    public static Order of( String clientOrderID, Side side ) {
        return new Order(clientOrderID, side);
    }

    public Instant getTimestamp() { return timestamp; }

    public Side getSide() { return side; }

    public int getOrderQty() {
        return orderQty;
    }

    public Order setSide(Side side) {
        this.side = side;
        return this;
    }

    public Order setOrderQty(int orderQty) {
        this.orderQty = orderQty;
        return this;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public Order setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    public TimeInForce getTimeInForce() {
        return timeInForce;
    }

    public Order setTimeInForce(TimeInForce timeInForce) {
        this.timeInForce = timeInForce;
        return this;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public Order setOrderType(OrderType orderType) {
        this.orderType = orderType;
        return this;
    }

    public Order setPrice(double price) {
        this.price = price;
        return this;
    }

    public Order setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public int getCumQty() {
        return cumQty;
    }

    public Order setCumQty(int cumQty) {
        if ( cumQty > orderQty ) throw new IllegalArgumentException("Order is over executed!");
        this.cumQty = cumQty;
        return this;
    }

    public double getPrice() {
        return price;
    }

    public String getClientOrderID() {
        return clientOrderID;
    }

    @Override
    public Order clone() {
        return new Order(timestamp, clientOrderID, side, orderQty, cumQty, expirationDate, timeInForce, orderType, price);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Order{");
        sb.append("timestamp=").append(timestamp);
        sb.append(", clientOrderID='").append(clientOrderID).append('\'');
        sb.append(", side=").append(side);
        sb.append(", orderQty=").append(orderQty);
        sb.append(", cumQty=").append(cumQty);
        sb.append(", expirationDate=").append(expirationDate);
        sb.append(", timeInForce=").append(timeInForce);
        sb.append(", orderType=").append(orderType);
        sb.append(", price=").append(price);
        sb.append('}');
        return sb.toString();
    }
}
