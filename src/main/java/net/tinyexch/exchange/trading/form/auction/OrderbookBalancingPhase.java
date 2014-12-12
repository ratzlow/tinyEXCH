package net.tinyexch.exchange.trading.form.auction;

/**
 * In equities without market imbalance information an order book balancing phase takes place if there is a surplus.
 * Executable orders, which cannot be executed in the price determination phase, will be made available to the market
 * for a limited period of time. This surplus contains all order sizes. Orders are executed at the determined auction
 * price in the order book balancing phase. Orders of the respective equity can neither be changed nor deleted during
 * order book balancing
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-10
 */
public interface OrderbookBalancingPhase {
    void balance();
}
