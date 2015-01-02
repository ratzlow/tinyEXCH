package net.tinyexch.order;

/**
 * What happened to the incoming or matched order.
 *
 * @author ratzlow@gmail.com
 * @since 2015-01-02
 */
public enum ExecType {

    REJECTED,

    /**
     * (partial fill or fill)
     */
    TRADE
}
