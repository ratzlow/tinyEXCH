package net.tinyexch.exchange.market;

import net.tinyexch.exchange.trading.model.TradingModel;

import java.util.Set;

/**
 * Market model serves as basis for rules and regulations.
 *
 * This market model is only applicable for equities. Not considered market models are:
 *      - Xetra International Market
 *      - Xetra BEST
 *      - Continuous AuctionTradingModel
 *
 *
 * For equities it defines principles of
 *      - order matching rules
 *      - price determination rules
 *      - trading models
 *      - order prioritization
 *      - accepted order/quote types
 *      - transparency level
 *      - type & extent of info available to market participants
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 * @link chap 1
 */
public abstract class MarketModel {
    private Set<ProductType> coveredProductTypes;
    private Set<TradingModel> tradingModels;



    public Set<ProductType> getCoveredProductTypes() {
        return coveredProductTypes;
    }

    public void setCoveredProductTypes(Set<ProductType> coveredProductTypes) {
        this.coveredProductTypes = coveredProductTypes;
    }

    public Set<TradingModel> getTradingModels() {
        return tradingModels;
    }

    public void setTradingModels(Set<TradingModel> tradingModels) {
        this.tradingModels = tradingModels;
    }
}
