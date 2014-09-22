package net.tinyexch.exchange.runtime;

/**
 * // TODO (FRa) : (FRa) : comment
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-22
 */
@FunctionalInterface
public interface NotificationListener {
    void accept(MarketNotification notification);
}
