package net.tinyexch.exchange.runtime;

import net.tinyexch.exchange.trading.model.ContinuousTradingInterruptedByAuctions;
import net.tinyexch.exchange.trading.model.TradingModelProfile;

import java.util.function.Consumer;

/**
 * // TODO (FRa) : (FRa) : comment
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-22
 */
public class MarketRunner {

    private ContinuousTradingInterruptedByAuctions market;
    private TradingModelProfile tradingModelProfile;
    private Consumer<MarketNotification> notificationConsumer;

    //-----------------------------------------------------------
    //
    //-----------------------------------------------------------

    public void submit( MarketCommand cmd ) {
        cmd.execute( market );
    }


    //-----------------------------------------------------------
    // life cycle API
    //-----------------------------------------------------------

    public void init() {
        market = new ContinuousTradingInterruptedByAuctions( tradingModelProfile );
    }

    public void setMarket(ContinuousTradingInterruptedByAuctions market) {
        this.market = market;
    }

    public void setTradingModelProfile(TradingModelProfile tradingModelProfile) {
        this.tradingModelProfile = tradingModelProfile;
    }
}
