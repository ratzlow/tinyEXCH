package net.tinyexch.order;

/**
 * Specifies how sell side should work the order, especially how to match it.
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 * @link chap 2, 1.
 */
public enum OrderType {

    /**
     * Market orders are unlimited bid/ask orders. They are to be executed at the next price determined.
     */
    MARKET,

    /**
     * Limit orders are bid/ask orders, which are to be executed at their specified limit or better.
     */
    LIMIT,

    /**
     * Market-to-limit orders are unlimited bid/ask orders, which are to be executed at the auction price or
     * (in continuous trading) at the best limit in the order book, if this limit is represented by at least
     * one limit order and if there is no market order on the other side of the book. Any unexecuted part of a
     * market-to- limit order is entered into the order book with a limit equal to the price of the first
     * partial execution.
     */
    MARKET_TO_LIMIT,

    STOP,

    /** http://www.fixtradingcommunity.org/pg/discussions/topicpost/165920/xetra-ets-top-of-book */
    STRIKE_MATCH,

    TRAILING_STOP,
    ONE_CANCELS_OTHER,
    ODERS_ON_EVENT
}
