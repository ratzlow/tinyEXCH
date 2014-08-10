package net.tinyexch.exchange.trading.form.continuous;

import net.tinyexch.exchange.trading.form.TradingFormProvider;
import net.tinyexch.exchange.trading.form.auction.Auction;

/**
 * Handler to provide access to both trading forms executed subsequently in
 * {@link net.tinyexch.exchange.trading.model.ContinuousTradingInterruptedByAuctions}
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-04
 */
public class ContinuousTradingAuctionsProvider implements TradingFormProvider {

    private final ContinuousTrading continuousTrading;
    private final Auction auction;

    public ContinuousTradingAuctionsProvider(Auction auction, ContinuousTrading continuousTrading ) {
        this.continuousTrading = continuousTrading;
        this.auction = auction;
    }

    public ContinuousTrading getContinuousTrading() {
        return continuousTrading;
    }

    public Auction getAuction() {
        return auction;
    }
}
