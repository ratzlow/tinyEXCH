package net.tinyexch.ob;

import java.util.EnumSet;

import static net.tinyexch.ob.ProductType.*;

/**
 * // TODO (FRa) : (FRa) : commont
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 * @link chap 1
 */
public class EquityMarketModel extends MarketModel {

    public EquityMarketModel() {
        super(
            EnumSet.of(BOND, EQUITY, EQUITY_SUBSCRIPTION_RIGHT, ETF, ETP),
            EnumSet.of(TradingModel.AUCTION, TradingModel.CONTINUOUS_TRADING_WITH_AUCTION)
        );
    }
}
