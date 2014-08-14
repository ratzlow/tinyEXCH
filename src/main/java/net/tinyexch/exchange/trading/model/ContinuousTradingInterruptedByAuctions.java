package net.tinyexch.exchange.trading.model;

import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTradingAuctionsProvider;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTradingAuctionsStateChanger;

/**
 * // TODO (FRa) : (FRa) : Is this composite correctly designed? Are the market models stateful?
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-02
 */
public class ContinuousTradingInterruptedByAuctions
       extends TradingModel<ContinuousTradingAuctionsProvider, ContinuousTradingAuctionsStateChanger> {

    private Auction auction;
    private ContinuousTrading continuousTrading;


    public ContinuousTradingInterruptedByAuctions(TradingModelProfile profile) {
        super(profile);
    }

    @Override
    public void moveTo(ContinuousTradingAuctionsStateChanger stateChanger) {
        stateChanger.transition( new ContinuousTradingAuctionsProvider(auction, continuousTrading) );
    }
}
