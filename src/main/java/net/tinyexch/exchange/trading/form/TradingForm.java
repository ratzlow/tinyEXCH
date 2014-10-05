package net.tinyexch.exchange.trading.form;

import net.tinyexch.exchange.event.NotificationListener;
import net.tinyexch.exchange.event.produce.StateChangedEvent;
import net.tinyexch.ob.Orderbook;
import net.tinyexch.ob.match.MatchEngine;
import org.slf4j.Logger;

import java.util.*;

/**
 * Parent class for all trading forms supported. It provides a means to transition through the different stages of a
 * given trading form.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-02
 *
 * @param <S> state type of the concrete trading form
 */
// TODO (FRa) : (FRa) : InitialPublicOffering, OverTheCounter
public abstract class TradingForm<S extends Enum<S>> {

    private final Orderbook orderbook;
    protected final NotificationListener notificationListener;

    private final Map<S, Set<S>> allowedTransitions;
    private S currentState = getDefaultState();


    //--------------------------------------------------------------------------------------------------
    // constructors
    //--------------------------------------------------------------------------------------------------

    protected TradingForm(NotificationListener notificationListener, MatchEngine matchEngine) {
        this.orderbook = new Orderbook(matchEngine);
        this.allowedTransitions = getAllowedTransitions();
        this.notificationListener = notificationListener;
    }

    protected TradingForm( NotificationListener notificationListener ) {
        this.notificationListener = notificationListener;
        this.orderbook = new Orderbook();
        this.allowedTransitions = getAllowedTransitions();
    }

    //--------------------------------------------------------------------------------------------------
    // public API
    //--------------------------------------------------------------------------------------------------


    /**
     * Callback to switch through the life cycle of a trading form. Used to start, close a given
     * TradingForm or flip into intermediate state.
     * If the trading model is already in the specified target state this request ignored.
     *
     * @throws java.lang.IllegalStateException if invalid transition is attempted
     */
    public void transitionTo( S targetState ) {

        S previous = currentState;
        if ( targetState == currentState ) {
            getLogger().info("Ignore transition request as this {} is already in state {}",
                    getClass().getSimpleName(), targetState);
            return;
        }

        Set<S> nextAllowedStates = allowedTransitions.containsKey(currentState) ?
                allowedTransitions.get(currentState) :
                Collections.EMPTY_SET;
        if ( nextAllowedStates.contains(targetState) ) {
            getLogger().info("Change state from {} -> {}", currentState, targetState);
            currentState = targetState;

        } else {
            String msg = String.format("Cannot transition from current: '%s' -> new: '%s'! Next allowed new state is '%s'",
                    currentState, targetState, nextAllowedStates );
            throw new IllegalStateException(msg);
        }

        notificationListener.fire(new StateChangedEvent<>(previous, currentState));
    }

    public S getCurrentState() {
        return currentState;
    }

    /**
     * Describe the allowed transitions fromState -> toStates
     *
     * @return map key: from -> values: to
     */
    protected abstract Map<S, Set<S>> getAllowedTransitions();

    /**
     * @return the state a trading form should be in when it is created. Usually it should be
     *  some sort of inactive state
     */
    protected abstract S getDefaultState();

    /**
     * Stop the current trading form no matter in what current state it is.
     */
    public abstract void close();

    /**
     * @return from concrete class intialized
     */
    protected abstract Logger getLogger();


    protected Orderbook getOrderbook() {
        return orderbook;
    }
}
