package net.tinyexch.ob;

/**
 * Defines the specific reasons, why an order was not accepted by the exchange.
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-29
 */
public enum RejectReason {
    MIN_SIZE("Insufficient order size!"),
    GTD("Good til date is not within now - T+n"),
    ORDER_TYPE("The allowed order type is dependent on the trading model and market!"),
    INSUFFICIENT_OB_CONSTELLATION("The current market situation does not allow the order to be accepted!");

    private final String msg;

    RejectReason(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
