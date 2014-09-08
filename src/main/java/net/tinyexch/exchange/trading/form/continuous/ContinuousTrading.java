package net.tinyexch.exchange.trading.form.continuous;

import net.tinyexch.exchange.trading.form.TradingForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState.RUNNING;
import static net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState.STOPPED;

/**
 * Order/quote driven trading form where price is continually derived based on incoming orders and quotes. Opposite
 * orderbook sides are attempted to match immediately with every incoming order/quote.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-01
 */
public class ContinuousTrading extends TradingForm<ContinuousTradingState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContinuousTrading.class);
    @Override
    protected Map<ContinuousTradingState, Set<ContinuousTradingState>> getAllowedTransitions() {
        Map<ContinuousTradingState, Set<ContinuousTradingState>>allowedTransitions = new EnumMap<>(ContinuousTradingState.class);
        allowedTransitions.put(STOPPED, singleton(RUNNING));
        allowedTransitions.put(RUNNING, singleton(STOPPED));
        return allowedTransitions;
    }

    @Override
    public ContinuousTradingState getDefaultState() { return STOPPED; }

    @Override
    public void close() {
        transitionTo( ContinuousTradingState.STOPPED );
    }

    public void start() {
        transitionTo(ContinuousTradingState.RUNNING);
    }

    @Override
    protected Logger getLogger() { return LOGGER; }
}
