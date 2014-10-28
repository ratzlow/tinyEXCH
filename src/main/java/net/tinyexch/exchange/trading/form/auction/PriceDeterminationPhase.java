package net.tinyexch.exchange.trading.form.auction;

/**
 * Triggered after orderbook was closed. Follows the {@link net.tinyexch.exchange.trading.form.auction.CallPhase}
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-10
 */
public interface PriceDeterminationPhase {
    PriceDeterminationResult determinePrice();
}
