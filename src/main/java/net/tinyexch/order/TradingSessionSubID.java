package net.tinyexch.order;

/**
 * Optional market assigned sub identifier for a trading phase within a trading session. Usage is determined by
 * market or counterparties.
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-18
 * @link FIX:625
 */
public enum TradingSessionSubID {
    PreTrading,
    OpeningOrOpeningAuction,
    Continuous,
    ClosingOrClosingAuction,
    PostTrading,
    IntradayAuction,
    Quiescent;
}
