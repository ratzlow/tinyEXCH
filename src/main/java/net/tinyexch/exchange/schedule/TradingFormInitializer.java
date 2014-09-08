package net.tinyexch.exchange.schedule;

import net.tinyexch.exchange.trading.form.StateChangeListener;
import net.tinyexch.exchange.trading.model.TradingFormRunType;
import net.tinyexch.exchange.trading.model.TradingModel;

import java.util.List;

/**
 * Provide resources to create a new concrete trading form.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-06
 */
public abstract class TradingFormInitializer<T extends TradingModel> {

    private final TradingFormRunType tradingFormRuntType;

    public TradingFormInitializer(TradingFormRunType tradingFormRuntType) {
        this.tradingFormRuntType = tradingFormRuntType;
    }

    public TradingFormRunType getTradingFormRuntype() {
        return tradingFormRuntType;
    }

    public abstract void setup(T tradingModel, List<StateChangeListener> listeners);
}
