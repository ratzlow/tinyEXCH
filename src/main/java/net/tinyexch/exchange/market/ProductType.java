package net.tinyexch.exchange.market;

/**
 * Products traded at the exchange. Different products are based on different legal obligations and trading forms.
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 * @link chap 1
 */
public enum ProductType {
    EQUITY,
    EQUITY_SUBSCRIPTION_RIGHT,

    /**
     * "Exchange Traded Fund"
     */
    ETF,

    /**
     * "Exchange Traded Product" Includes ETC ("Exchange Traded Commodities") & ETN ("Exchange Traded Notes")
     */
    ETP,

    BOND
}
