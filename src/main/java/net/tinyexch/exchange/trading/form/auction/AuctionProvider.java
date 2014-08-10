package net.tinyexch.exchange.trading.form.auction;

import net.tinyexch.exchange.trading.form.TradingFormProvider;

/**
 * If you are running the trading model {@link net.tinyexch.exchange.trading.model.AuctionTradingModel} this class
 * returns the underlying auction.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-03
 */
public class AuctionProvider implements TradingFormProvider {
    private final Auction auction;

    public AuctionProvider(Auction auction) {
        this.auction = auction;
    }

    public Auction getAuction() {
        return auction;
    }
}
