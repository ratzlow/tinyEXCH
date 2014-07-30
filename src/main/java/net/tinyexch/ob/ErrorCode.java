package net.tinyexch.ob;

import java.util.Optional;

/**
 * Could the order be submitted to the {@link net.tinyexch.ob.Orderbook}
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-27
 */
public class ErrorCode {
    public enum Type { ERROR, REJECT, OK}

    public final Type type;
    public final Order order;
    public final String hint;
    public final Optional<RejectReason> rejectReason;

    //-----------------------------------------------------
    // constructors
    //-----------------------------------------------------

    public ErrorCode(Type type, Order order, String hint) {
        this(type, order, hint, null );
    }

    public ErrorCode(Type type, Order order, String hint, RejectReason rejectReason) {
        this.order = order;
        this.type = type;
        this.hint = hint;
        this.rejectReason = Optional.ofNullable(rejectReason);
    }
}
