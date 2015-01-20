package net.tinyexch.exchange.event.produce;

import net.tinyexch.order.Trade;

/**
 * Emitted as a result of a full or partial order match.
 *
 * @author ratzlow@gmail.com
 * @since 2015-01-18
 */
public class NewTradeEvent {
    private final Trade trade;

    public NewTradeEvent(Trade trade) {
        this.trade = trade;
    }

    public Trade getTrade() {
        return trade;
    }
}
