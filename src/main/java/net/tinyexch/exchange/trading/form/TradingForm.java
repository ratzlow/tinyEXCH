package net.tinyexch.exchange.trading.form;

import net.tinyexch.ob.Orderbook;
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
public abstract class TradingForm<S extends Enum<S>> {

    protected final Orderbook orderbook = new Orderbook();
    private final List<StateChangeListener<S>> stateChangeListeners = new ArrayList<>();
    private final Map<S, Set<S>> allowedTransitions;
    private S currentState = getDefaultState();

    //--------------------------------------------------------------------------------------------------
    // constructors
    //--------------------------------------------------------------------------------------------------

    protected TradingForm( List<StateChangeListener<S>> stateChangeListeners ) {
        Objects.requireNonNull(stateChangeListeners);
        this.allowedTransitions = getAllowedTransitions();
        this.stateChangeListeners.addAll(stateChangeListeners);
    }

    protected TradingForm() {
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
    protected void transitionTo( S targetState ) {

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

        stateChangeListeners.stream().forEach(listener -> listener.stateChanged(targetState));
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

    /**
     * @param listener another listener which will fire if the state of the current trading form changes
     */
    public void register( StateChangeListener<S> listener ){
        Objects.requireNonNull(listener);
        stateChangeListeners.add( listener );
    }
}
