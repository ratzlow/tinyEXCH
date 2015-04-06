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

    private long submitSequence = -1;

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

    //----START: iceberg fields
    /** @link FIX:1138 */
    private int displayQty;

    /** full qty of an iceberg at submit time */
    private int icebergOrderQty;

    /** done size of overall iceberg */
    private int icebergCumQty;
    //----END: iceberg fields

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
    private Order(long submitSequence, Instant timestamp, DiscretionLimitType discretionLimitType, TradingSessionSubID tradingSessionSubID,
                 String clientOrderID, Side side, int orderQty, int cumQty, LocalDateTime expirationDate,
                 TimeInForce timeInForce, OrderType orderType, double price, double stopPrice, int displayQty,
                 int icebergOrderQty, int icebergCumQty) {
        this.submitSequence = submitSequence;
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
        this.displayQty = displayQty;
        this.icebergOrderQty = icebergOrderQty;
        this.icebergCumQty = icebergCumQty;
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

    public int getLeavesQty() { return orderQty - cumQty; }

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

    public Order setCumQty( int cumQty ) {
        if ( cumQty > orderQty ) {
            String msg = String.format("Order is over executed! cumQty=%d orderQty=%d", cumQty, orderQty);
            throw new IllegalArgumentException(msg);
        }

        this.cumQty = cumQty;

        return this;
    }

    public Order setCumQty( int cumQty, Instant ts ) {
        setCumQty( cumQty );

        if ( isIceberg() ) {
            if ( icebergCumQty < cumQty ) {
                icebergCumQty = cumQty;
            } else {
                int doneSlices = icebergCumQty / displayQty;
                icebergCumQty = doneSlices * displayQty + cumQty;
            }

            // expose new slice: either visible or remaining qty less than the visible size
            if ( getLeavesQty() == 0 ) {
                orderQty = Math.min( displayQty, icebergOrderQty - icebergCumQty );
                this.cumQty = 0;
                this.timestamp = ts;
            }
        }

        return this;
    }



    public Order setDisplayQty(int displayQty) {
        this.displayQty = displayQty;
        this.icebergOrderQty = orderQty;
        this.orderQty = displayQty;
        return this;
    }

    /** @return qty of shares not yet exposed to the market of an iceberg order */
    public int getHiddenQty() {
        return getIcebergOrderQty() - getIcebergCumQty() - getLeavesQty();
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

    public boolean isIceberg() {
        return icebergOrderQty > 0;
    }

    public int getIcebergCumQty() {
        return icebergCumQty;
    }

    public int getIcebergOrderQty() { return icebergOrderQty; }

    public int getDisplayQty() {
        return displayQty;
    }

    public Order setSubmitSequence( long sequence ) {
        this.submitSequence = sequence;
        return this;
    }

    public long getSubmitSequence() {
        return submitSequence;
    }

    public Order mutableClone() {
        return new Order(submitSequence, timestamp, discretionLimitType, tradingSessionSubID, clientOrderID, side, orderQty, cumQty,
                expirationDate, timeInForce, orderType, price, stopPrice, displayQty, icebergOrderQty, icebergCumQty );
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Order{");
        sb.append("timestamp=").append(timestamp);
        sb.append(", submitSequence=").append(submitSequence);
        sb.append(", discretionLimitType=").append(discretionLimitType);
        sb.append(", tradingSessionSubID=").append(tradingSessionSubID);
        sb.append(", clientOrderID='").append(clientOrderID).append('\'');
        sb.append(", side=").append(side);
        sb.append(", orderQty=").append(orderQty);
        sb.append(", cumQty=").append(cumQty);
        sb.append(", displayQty=").append(displayQty);
        sb.append(", icebergOrderQty=").append(icebergOrderQty);
        sb.append(", icebergCumQty=").append(icebergCumQty);
        sb.append(", expirationDate=").append(expirationDate);
        sb.append(", timeInForce=").append(timeInForce);
        sb.append(", orderType=").append(orderType);
        sb.append(", price=").append(price);
        sb.append(", stopPrice=").append(stopPrice);
        sb.append('}');
        return sb.toString();
    }
}
