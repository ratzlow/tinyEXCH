package net.tinyexch.ob;

/**
 * The modification type to the orderbook to be accomplished with a given order.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-28
 */
public enum SubmitType {
    NEW,

    // TODO (FRa) : (FRa) : check if MOD & REPL are same action and always resulting in new order ID
    MODIFY,

    CANCEL
}
