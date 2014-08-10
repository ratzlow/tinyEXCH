package net.tinyexch.exchange.trading.form;

/**
 * Will be fired if the state of the monitored trading form changed to a different phase.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-09
 */
@FunctionalInterface
public interface StateChangeListener<S extends Enum<S>> {
    void stateChanged( S newState );
}
