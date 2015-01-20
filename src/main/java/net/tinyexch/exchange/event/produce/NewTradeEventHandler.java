package net.tinyexch.exchange.event.produce;

/**
 * Will sent the trades to the counter parties of the orders.
 *
 * @author ratzlow@gmail.com
 * @since 2015-01-18
 */
public class NewTradeEventHandler {

    public void handle(NewTradeEvent event) {
        // TODO (FRa) : (FRa) : invoke adapters of counterparties to send out trade msgs
    }
}
