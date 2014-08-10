package net.tinyexch.exchange.trading.form;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

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

    private final static Logger LOGGER = LoggerFactory.getLogger(TradingForm.class);

    private final StateChangeListener<S> stateChangeListener;
    private final Map<S, Set<S>> allowedTransitions;
    private S currentState = getDefaultState();

    //--------------------------------------------------------------------------------------------------
    // constructors
    //--------------------------------------------------------------------------------------------------

    protected TradingForm( StateChangeListener<S> stateChangeListener ) {
        this.allowedTransitions = getAllowedTransitions();
        this.stateChangeListener = stateChangeListener;
    }

    protected TradingForm() {
        this.allowedTransitions = getAllowedTransitions();
        this.stateChangeListener = currentState -> {};
    }

    //--------------------------------------------------------------------------------------------------
    // public API
    //--------------------------------------------------------------------------------------------------

    /**
     * Callback to switch through the life cycle of a trading form. Used to start, stop a given
     * TradingForm or flip into intermediate state.
     * If the trading model is already in the specified target state this request ignored.
     *
     * @throws java.lang.IllegalStateException if invalid transition is attempted
     */
    protected void transitionTo( S targetState ) {

        if ( targetState == currentState ) {
            LOGGER.info("Ignore transition request as this {} is already in state {}",
                    getClass().getSimpleName(), targetState);
            return;
        }

        Set<S> nextAllowedStates = allowedTransitions.get(currentState);
        if ( nextAllowedStates.contains(targetState) ) {
            LOGGER.info("Change state from {} -> {}", currentState, targetState);
            currentState = targetState;

        } else {
            String msg = String.format("Cannot transition from current: '%s' -> new:%s! Next allowed new state is '%s'",
                    currentState, targetState, nextAllowedStates );
            throw new IllegalStateException(msg);
        }

        stateChangeListener.stateChanged( currentState );
    }

    public S getCurrentState() {
        return currentState;
    }

    /**
     * Describe the allowed transitions fromState -> toState
     *
     * @return map key: from -> value: to
     */
    protected abstract Map<S, Set<S>> getAllowedTransitions();


    /**
     * @return the state a trading form should be in when it is created. Usually it should be
     *  some sort of inactive state
     */
    protected abstract S getDefaultState();
}
