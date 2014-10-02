package net.tinyexch.exchange.trading.model;

import net.tinyexch.exchange.event.NotificationListener;
import net.tinyexch.exchange.event.produce.TradingFormRunTypeChangedEvent;
import org.slf4j.Logger;

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
    private final NotificationListener notificationListener;
    private TradingFormRunType tradingFormRunType;

    //--------------------------------------------------------------------------------
    // constructors
    //--------------------------------------------------------------------------------

    protected TradingModel(TradingModelProfile profile, NotificationListener notificationListener ) {
        this.profile = profile;
        this.notificationListener = notificationListener;
    }


    //--------------------------------------------------------------------------------
    // pub API
    //--------------------------------------------------------------------------------


    public TradingFormRunType getTradingFormRunType() {
        return tradingFormRunType;
    }

    protected abstract Logger getLogger();

    public void setTradingFormRunType(TradingFormRunType tradingFormRunType) {
        TradingFormRunType previous = this.tradingFormRunType;
        getLogger().info("Change tradingFromRunType from {} -> {}", this.tradingFormRunType, tradingFormRunType);
        this.tradingFormRunType = tradingFormRunType;
        notificationListener.fire(new TradingFormRunTypeChangedEvent(previous, tradingFormRunType));
    }
}
