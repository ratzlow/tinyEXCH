package net.tinyexch.exchange.trading.form.auction;

/**
 * The sequence of states reflects the life cycle of an auction. This is independent of the particular
 * {@link AuctionType}
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-03
 * @link chap 10.1.1
 */
public enum AuctionState {

    /**
     * Market partici- pants are able to enter orders and quotes in this phase as well as modify and delete
     * their own existing orders and quotes.
     */
    CALL_RUNNING(true),
    CALL_STOPPED(false),


    /**
     * The auction price is determined according to the principle of most executable volume on the basis of
     * the order book situation at the end of the call phase.
     */
    PRICE_DETERMINATION_RUNNING(true),
    PRICE_DETERMINATION_STOPPED(false),

    /**
     * Orders are executed at the determined auction price in the order book balancing phase.
     */
    ORDERBOOK_BALANCING_RUNNING(true),
    ORDERBOOK_BALANCING_STOPPED(false),

    /** The auction is currently not running */
    INACTIVE(false);


    private final boolean active;

    AuctionState(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * @return get the first active state of an auction lifecycle
     */
    public static AuctionState start() { return values()[0]; }

    /**
     * @return get the last state after the auction is done and closed
     */
    public static AuctionState close() { return values()[values().length - 1]; }
}
