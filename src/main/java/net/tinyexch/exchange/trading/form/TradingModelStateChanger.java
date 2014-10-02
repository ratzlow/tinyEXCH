package net.tinyexch.exchange.trading.form;

/**
 * Visitor that switches the state of the visited TradingForm.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-03
 */
// TODO (FRa) : (FRa) : convert to functional interface?!
@Deprecated
public interface TradingModelStateChanger<T> {
    void transition( T tradingModel );
}
