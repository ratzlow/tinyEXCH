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
public class Order {

    private Instant timestamp = Instant.now();

    /** @link FIX:54 */
    private Side side;

    /** @link FIX:38 */
    private int orderQty;

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


    //---------------------------------------------------------
    // constructors
    //---------------------------------------------------------

    public Order() { }

    public Order( Side side ) {
        this.side = side;
    }

    //---------------------------------------------------------
    // accessors
    //---------------------------------------------------------

    public static Order of( Side side ) {
        return new Order(side);
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
}
