package net.tinyexch.exchange.trading.model;

import net.tinyexch.exchange.trading.form.StateChangeListener;
import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.auction.AuctionProvider;
import net.tinyexch.ob.OrderReceiver;
import net.tinyexch.ob.SubmitType;
import net.tinyexch.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

/**
 * In this model we execute one or more auctions.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-01
 */
public class AuctionTradingModel extends TradingModel implements OrderReceiver, AuctionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionTradingModel.class);
    private Auction auction;

    public AuctionTradingModel(TradingModelProfile profile) {
        super(profile);
    }

    @Override
    protected Logger getLogger() { return LOGGER; }

    @Override
    public void submit( Order order, SubmitType submitType ) {
        throw new IllegalStateException("Not implemented yet!");
    }

    @Override
    public Auction getAuction() {
        return auction;
    }

    public void initAuction(Supplier<Auction> supplier, TradingFormRunType runType, List<StateChangeListener> listeners) {
        setTradingFormRunType(runType);
        auction = supplier.get();
        listeners.forEach( auction::register );
    }
}
