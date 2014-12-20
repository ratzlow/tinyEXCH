package net.tinyexch.order;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Order to be matched against the other side of the orderbook. For more information regarding the FIX semantic
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-27
 * @link http://www.onixs.biz/fix-dictionary
 */
// TODO (FRa) : (FRa) : provide immutable clone() result
public class Order {

    private Instant timestamp = Instant.now();

    private DiscretionLimitType discretionLimitType;

    private TradingSessionSubID tradingSessionSubID;

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

    private TimeInForce timeInForce = TimeInForce.DAY;


    /** @link FIX:40 */
    private OrderType orderType;

    /**
     * Required for limit OrdTypes.
     * @link FIX:44
     */
    private double price;

    /**
     * Required for OrdType = "Stop" or OrdType = "Stop limit".
     * @link FIX:99
     */
    private double stopPrice;


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
    private Order(Instant timestamp, DiscretionLimitType discretionLimitType, TradingSessionSubID tradingSessionSubID,
                 String clientOrderID, Side side, int orderQty, int cumQty, LocalDateTime expirationDate,
                 TimeInForce timeInForce, OrderType orderType, double price, double stopPrice) {
        this.timestamp = timestamp;
        this.discretionLimitType = discretionLimitType;
        this.tradingSessionSubID = tradingSessionSubID;
        this.clientOrderID = clientOrderID;
        this.side = side;
        this.orderQty = orderQty;
        this.cumQty = cumQty;
        this.expirationDate = expirationDate;
        this.timeInForce = timeInForce;
        this.orderType = orderType;
        this.price = price;
        this.stopPrice = stopPrice;
    }

    //---------------------------------------------------------
    // set/get property
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

    public DiscretionLimitType getDiscretionLimitType() {
        return discretionLimitType;
    }

    public Order setDiscretionLimitType(DiscretionLimitType discretionLimitType) {
        this.discretionLimitType = discretionLimitType;
        return this;
    }

    public TradingSessionSubID getTradingSessionSubID() {
        return tradingSessionSubID;
    }

    public Order setTradingSessionSubID(TradingSessionSubID tradingSessionSubID) {
        this.tradingSessionSubID = tradingSessionSubID;
        return this;
    }

    public double getPrice() {
        return price;
    }

    public String getClientOrderID() {
        return clientOrderID;
    }

    public double getStopPrice() {
        return stopPrice;
    }

    public Order setStopPrice(double stopPrice) {
        this.stopPrice = stopPrice;
        return this;
    }

    public Order mutableClone() {
        return new Order(timestamp, discretionLimitType, tradingSessionSubID, clientOrderID, side, orderQty, cumQty,
                expirationDate, timeInForce, orderType, price, stopPrice);
    }

    @Override
    public String toString() {
        return "Order{" +
                "timestamp=" + timestamp +
                ", discretionLimitType=" + discretionLimitType +
                ", tradingSessionSubID=" + tradingSessionSubID +
                ", clientOrderID='" + clientOrderID + '\'' +
                ", side=" + side +
                ", orderQty=" + orderQty +
                ", cumQty=" + cumQty +
                ", expirationDate=" + expirationDate +
                ", timeInForce=" + timeInForce +
                ", orderType=" + orderType +
                ", price=" + price +
                ", stopPrice=" + stopPrice +
                '}';
    }
}
