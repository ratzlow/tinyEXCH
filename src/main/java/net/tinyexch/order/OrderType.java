package net.tinyexch.order;

/**
 * Specifies how sell side should work the order, especially how to match it.
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 * @link chap 2, 1.
 */
public enum OrderType {

    MARKET,
    LIMIT,
    MARKET_TO_LIMIT,
    STOP,
    ICEBERG,
    HIDDEN,
    MIDPOINT,

    /** http://www.fixtradingcommunity.org/pg/discussions/topicpost/165920/xetra-ets-top-of-book */
    STRIKE_MATCH,

    TRAILING_STOP,
    ONE_CANCELS_OTHER,
    ODERS_ON_EVENT
}
