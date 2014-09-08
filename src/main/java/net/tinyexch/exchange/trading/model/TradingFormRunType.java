package net.tinyexch.exchange.trading.model;

/**
 * Marks the currently run mode of a {@link net.tinyexch.exchange.trading.form.TradingForm} being part of a given
 * {@link net.tinyexch.exchange.trading.model.TradingModel}. The same trading form can run in a different mode.
 *
 * Rules might be associated with a given type.
 *
 * Examples:
 * "opening auction" and "closing auction" -> {@link net.tinyexch.exchange.trading.form.auction.Auction}
 * "continuous trading" -> {@link net.tinyexch.exchange.trading.form.continuous.ContinuousTrading}
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 * @link chap 2, 3.
 */
public enum TradingFormRunType {

    OPENING_AUCTION,

    /**
     * Can be scheduled or unscheduled in response to particular events e.g. volatility interruptions
     */
    INTRADAY_AUCTION,


    CLOSING_AUCTION,

    /**
     * End Of Day auction
     */
    EOD_AUCTION,

    /**
     * Order/quote driven trading form. There is only this one type for
     * {@link net.tinyexch.exchange.trading.form.continuous.ContinuousTrading}
     */
    CONTINUOUS_TRADING
}
