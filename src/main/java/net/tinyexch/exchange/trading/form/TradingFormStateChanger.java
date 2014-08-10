package net.tinyexch.exchange.trading.form;

/**
 * Visitor that switches the state of the visited TradingForm.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-03
 */
public interface TradingFormStateChanger<T extends TradingFormProvider> {
    void transition( T provider );
}
