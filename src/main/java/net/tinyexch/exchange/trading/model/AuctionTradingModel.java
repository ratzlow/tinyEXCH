package net.tinyexch.exchange.trading.model;

import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.auction.AuctionProvider;
import net.tinyexch.exchange.trading.form.auction.AuctionStateChanger;

/**
 * In this model we execute one or more auctions.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-01
 */
public class AuctionTradingModel
        extends TradingModel<AuctionProvider, AuctionStateChanger> {

    private final Auction auction;
    private final AuctionProvider auctionProvider;

    public AuctionTradingModel(TradingModelProfile profile, Auction auction) {
        super(profile);
        this.auction = auction;
        this.auctionProvider = new AuctionProvider(auction);
    }

    @Override
    public void transitionTradingForm(AuctionStateChanger stateChanger) {
        stateChanger.transition( auctionProvider );
    }
}
