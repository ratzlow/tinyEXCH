package net.tinyexch.ob;

import java.util.Collections;
import java.util.Set;

/**
 * Market model serves as basis for rules and regulations.
 *
 * This market model is only applicable for equities. Not considered market models are:
 *      - Xetra International Market
 *      - Xetra BEST
 *      - Continuous Auction
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
    private final Set<ProductType> productTypes;
    private final Set<TradingModel> tradingModelTypes;

    protected MarketModel(Set<ProductType> productTypes, Set<TradingModel> tradingModelTypes) {
        this.productTypes = Collections.unmodifiableSet( productTypes );
        this.tradingModelTypes = Collections.unmodifiableSet(tradingModelTypes);
    }

    public Set<ProductType> getProductTypes() {
        return productTypes;
    }

    public Set<TradingModel> getTradingModels() {
        return tradingModelTypes;
    }
}
