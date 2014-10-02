package net.tinyexch.exchange.event.produce;

import net.tinyexch.exchange.trading.model.TradingFormRunType;

/**
 * Emitted when the trading forms within a market model changed.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-23
 */
public class TradingFormRunTypeChangedEvent {
    private final TradingFormRunType previous;
    private final TradingFormRunType current;

    public TradingFormRunTypeChangedEvent(TradingFormRunType previous, TradingFormRunType current) {
        this.previous = previous;
        this.current = current;
    }

    public TradingFormRunType getPrevious() {
        return previous;
    }

    public TradingFormRunType getCurrent() {
        return current;
    }
}
