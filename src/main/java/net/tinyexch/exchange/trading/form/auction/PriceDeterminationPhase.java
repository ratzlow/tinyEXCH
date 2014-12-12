package net.tinyexch.exchange.trading.form.auction;

/**
 * Triggered after orderbook was closed. Follows the {@link net.tinyexch.exchange.trading.form.auction.CallPhase}
 *
 * The auction price is determined according to the principle of most executable volume on the basis of the
 * order book situation at the end of the call phase.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-10
 */
public interface PriceDeterminationPhase {
    PriceDeterminationResult determinePrice();
}
