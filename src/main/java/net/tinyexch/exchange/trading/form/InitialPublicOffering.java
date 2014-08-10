package net.tinyexch.exchange.trading.form;

/**
 * IPO
 *
 * // TODO (FRa) : (FRa) : check docs and
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-02
 */
public class InitialPublicOffering extends TradingForm {
    @Override
    protected java.util.Map<net.tinyexch.exchange.trading.form.auction.AuctionState, net.tinyexch.exchange.trading.form.auction.AuctionState> getAllowedTransitions() {
        throw new IllegalStateException("// TODO (FRa) : (FRa) : not yet implemented");
    }

    @Override
    protected Enum getDefaultState() {
        throw new IllegalStateException("// TODO (FRa) : (FRa) : not yet implemented");
    }
}
