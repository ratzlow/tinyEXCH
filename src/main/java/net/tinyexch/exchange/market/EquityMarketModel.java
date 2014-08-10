package net.tinyexch.exchange.market;

import net.tinyexch.exchange.trading.model.AuctionTradingModel;
import net.tinyexch.exchange.trading.model.ContinuousTradingInterruptedByAuctions;

import java.util.EnumSet;

import static net.tinyexch.exchange.market.ProductType.*;

/**
 * Configuration suitable for trading equities. See listed ProductTypes.
 *
 * // TODO (FRa) : (FRa) : move to generic config class?: auction, continuousTradingInterruptedByAuctions
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 * @link chap 1
 */
public class EquityMarketModel extends MarketModel {

    public EquityMarketModel( AuctionTradingModel auction,
                              ContinuousTradingInterruptedByAuctions continuousTradingInterruptedByAuctions ) {

        setCoveredProductTypes( EnumSet.of(BOND, EQUITY, EQUITY_SUBSCRIPTION_RIGHT, ETF, ETP) );
    }
}
