package net.tinyexch.exchange.event;

import net.tinyexch.exchange.schedule.TradingCalendar;

/**
 * Registered with {@link net.tinyexch.exchange.trading.model.TradingModel} and
 * {@link net.tinyexch.exchange.trading.form.TradingForm} intercepting all emitted events as a result of market activities.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-22
 */
public interface NotificationListener {

    /**
     * Default do nothing listener.
     */
    NotificationListener NO_OP = new NotificationListener() {
        @Override
        public void init(MarketRunner marketRunner, TradingCalendar tradingCalendar) { }

        @Override
        public <T> void fire( T event ) { }
    };


    /**
     * Invoked before listener can receive events.
     *
     * @param marketRunner in control of the market this listener is wired to
     * @param tradingCalendar containing the schedule
     */
    void init(MarketRunner marketRunner, TradingCalendar tradingCalendar );


    /**
     * Receives events from throughout the application.
     *
     * @param notification emitted event in response to some action
     * @param <T>
     */
    <T> void fire( T notification );
}
