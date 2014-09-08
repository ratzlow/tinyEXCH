package net.tinyexch.exchange.trading.form;

import org.slf4j.Logger;

/**
 * OTC
 *
 * // TODO (FRa) : (FRa) : comment
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-02
 */
public class OverTheCounter extends TradingForm {
    @Override
    protected java.util.Map<net.tinyexch.exchange.trading.form.auction.AuctionState, net.tinyexch.exchange.trading.form.auction.AuctionState> getAllowedTransitions() {
        throw new IllegalStateException("// TODO (FRa) : (FRa) : not yet implemented");
    }

    @Override
    protected Enum getDefaultState() {
        throw new IllegalStateException("// TODO (FRa) : (FRa) : not yet implemented");
    }

    @Override
    public void close() {
        throw new IllegalStateException("// TODO (FRa) : (FRa) : not yet implemented");
    }

    @Override
    protected Logger getLogger() {
        throw new IllegalStateException("// TODO (FRa) : (FRa) : not yet implemented");
    }
}
