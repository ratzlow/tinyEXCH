package net.tinyexch.exchange.trading.model;

import net.tinyexch.exchange.trading.form.StateChangeListener;
import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.auction.AuctionProvider;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

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

    private Auction auction;
    private ContinuousTrading continuousTrading;


    public ContinuousTradingInterruptedByAuctions(TradingModelProfile profile) {
        super(profile);
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

    public void initAuction(Supplier<Auction> auctionSupplier, TradingFormRunType tradingFormRunType, List<StateChangeListener> listeners) {
        continuousTrading = null;
        setTradingFormRunType(tradingFormRunType);
        auction = auctionSupplier.get();
        listeners.forEach(auction::register);
    }

    public void initContinuousTrading(Supplier<ContinuousTrading> continuousTradingSupplier,
                                      TradingFormRunType tradingFormRunType, List<StateChangeListener> listeners) {
        auction = null;
        setTradingFormRunType(tradingFormRunType);
        continuousTrading = continuousTradingSupplier.get();
        listeners.forEach(continuousTrading::register);
    }
}
