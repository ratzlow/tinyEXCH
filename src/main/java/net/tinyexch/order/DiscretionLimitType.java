package net.tinyexch.order;

/**
 * @author ratzlow@gmail.com
 * @since 2014-12-18
 * @link FIX:843
 */
public enum DiscretionLimitType {

    /** 0 = Or better (default) - price improvement allowed */
    OR_BETTER,

    /** 1 = Strict - limit is a strict limit */
    STRICT,

    /**
     * 2 = Or worse - for a buy the discretion price is a minimum and for a sell the discretion price is a
     * maximum (for use for orders which have a price range)
     */
    OR_WORSE
}
