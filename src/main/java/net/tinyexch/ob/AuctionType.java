package net.tinyexch.ob;

/**
 * // TODO (FRa) : (FRa) : check how to configure trading cycle: use TradingCalendar?
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 * @link chap 2, 3.
 */
public enum AuctionType {
    OPENING,
    INTRADAY,
    CLOSING,

    /**
     * end of day
     */
    EOD
}
