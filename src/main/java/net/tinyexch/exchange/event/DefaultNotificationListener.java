package net.tinyexch.exchange.event;


import net.tinyexch.exchange.event.produce.StateChangedEvent;
import net.tinyexch.exchange.event.produce.StateChangedEventHandler;
import net.tinyexch.exchange.event.produce.TradingFormRunTypeChangedEvent;
import net.tinyexch.exchange.event.produce.VolatilityInterruptionEventHandler;
import net.tinyexch.exchange.schedule.TradingCalendar;
import net.tinyexch.exchange.trading.model.TradingFormRunType;
import net.tinyexch.ob.price.safeguard.VolatilityInterruption;

import java.util.ArrayList;
import java.util.List;

/**
 * Receives all events raised when a trading form runs - or multiple if e.g. an auction and continuous trading are
 * switching. You can register event handlers for particular event types to control actions upon receipt.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-23
 */
public class DefaultNotificationListener implements NotificationListener {

    private List<TradingFormRunType> tradingFormRunTypes = new ArrayList<>();
    private StateChangedEventHandler stateChangedEventHandler;
    private VolatilityInterruptionEventHandler volatilityInterruptionEventHandler;


    @Override
    public void init( MarketRunner marketRunner, TradingCalendar tradingCalendar ) {
        stateChangedEventHandler.init( marketRunner, tradingCalendar );
    }


    @Override
    public <T> void fire(T notification) {
        if (notification instanceof StateChangedEvent) {
            stateChangedEventHandler.handle((StateChangedEvent) notification);

        } else if (notification instanceof TradingFormRunTypeChangedEvent) {
            process((TradingFormRunTypeChangedEvent) notification);

        } else if (notification instanceof VolatilityInterruption) {
            volatilityInterruptionEventHandler.handle((VolatilityInterruption) notification);

        } else throw new IllegalStateException("Unmapped notification received! " + notification);
    }


    private void process( TradingFormRunTypeChangedEvent event ) {
        tradingFormRunTypes.add(event.getCurrent());
    }


    //
    // setting handlers
    //

    public void setStateChangedEventHandler(StateChangedEventHandler stateChangedEventHandler) {
        this.stateChangedEventHandler = stateChangedEventHandler;
    }

    public void setVolatilityInterruptionEventHandler(VolatilityInterruptionEventHandler volatilityInterruptionEventHandler) {
        this.volatilityInterruptionEventHandler = volatilityInterruptionEventHandler;
    }

    public List<TradingFormRunType> getTradingFormRunTypes() { return tradingFormRunTypes; }
}
