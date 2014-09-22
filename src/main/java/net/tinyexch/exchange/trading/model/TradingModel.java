package net.tinyexch.exchange.trading.model;

import net.tinyexch.exchange.schedule.TradingFormInitializer;
import net.tinyexch.exchange.trading.form.StateChangeListener;
import org.slf4j.Logger;

import java.util.List;

/**
 * Specifies how the trading process for a given security should be executed. It defines the sequence and kind of
 * {@link net.tinyexch.exchange.trading.form.TradingForm}
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 * @link chap 2, 2.
 */
public abstract class TradingModel {

    private final TradingModelProfile profile;
    private TradingFormRunType tradingFormRunType;

    //--------------------------------------------------------------------------------
    // constructors
    //--------------------------------------------------------------------------------

    protected TradingModel(TradingModelProfile profile) {
        this.profile = profile;
    }


    //--------------------------------------------------------------------------------
    // pub API
    //--------------------------------------------------------------------------------


    public TradingFormRunType getTradingFormRunType() {
        return tradingFormRunType;
    }

    protected abstract Logger getLogger();

    protected void setTradingFormRunType(TradingFormRunType tradingFormRunType) {
        getLogger().info("Change tradingFromRunType from {} -> {}", this.tradingFormRunType, tradingFormRunType);
        this.tradingFormRunType = tradingFormRunType;
    }

    public void init(TradingFormInitializer initializer, List<StateChangeListener> listeners) {
        initializer.setup(this, listeners);
    }

    protected TradingModelProfile getProfile() {
        return profile;
    }
}
