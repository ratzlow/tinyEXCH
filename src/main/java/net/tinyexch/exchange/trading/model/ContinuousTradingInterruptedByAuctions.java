package net.tinyexch.exchange.trading.model;

import net.tinyexch.exchange.event.NotificationListener;
import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.auction.AuctionProvider;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * // TODO (FRa) : (FRa) : Is this composite correctly designed? Are the market models stateful?
 * // TODO (FRa) : (FRa) : rules for opening/closing auctions are certainly different from interrupting auctions
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-02
 */
public class ContinuousTradingInterruptedByAuctions
       extends TradingModel implements AuctionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContinuousTradingInterruptedByAuctions.class);

    private final Auction auction;
    private final ContinuousTrading continuousTrading;


    public ContinuousTradingInterruptedByAuctions(TradingModelProfile profile, NotificationListener notificationListener,
                                                  ContinuousTrading continuousTrading, Auction auction) {
        super(profile, notificationListener);
        this.auction = auction;
        this.continuousTrading = continuousTrading;
    }

    @Override
    protected Logger getLogger() { return LOGGER; }

    @Override
    public Auction getAuction() {
        return auction;
    }

    public ContinuousTrading getContinuousTrading() {
        return continuousTrading;
    }
}
