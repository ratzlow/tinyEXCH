package net.tinyexch.exchange.schedule;

import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.auction.AuctionStateChanger;
import net.tinyexch.exchange.trading.model.AuctionTradingModel;
import net.tinyexch.exchange.trading.model.TradingModelProfile;
import org.junit.Test;

import java.util.Arrays;

/**
 * Check that we can specify the timing and order of transition of trading model phases and between trading forms,
 * e.g. Auction -> ContinuousTrading -> Auction
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-10
 */
public class TradingModelScheduledStateChangeTest {
    
    @Test
    public void testAuctionSchedule() {
        TradingModelProfile profile = new TradingModelProfile();
        Auction auction = new Auction();
        AuctionTradingModel auctionTradingModel = new AuctionTradingModel(profile, auction);
        Arrays.stream( AuctionStateChanger.values() ).forEach( auctionTradingModel::transitionTradingForm );
    }
}
