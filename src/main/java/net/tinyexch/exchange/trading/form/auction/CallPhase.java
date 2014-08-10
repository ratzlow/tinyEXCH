package net.tinyexch.exchange.trading.form.auction;

import net.tinyexch.order.Order;

/**
 * // TODO (FRa) : (FRa) : doc from spec
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-10
 */
@FunctionalInterface
public interface CallPhase {
    void accept( Order order );
}
