package net.tinyexch.exchange.trading.form.continuous;

import net.tinyexch.exchange.trading.form.TradingForm;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState.RUNNING;
import static net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState.STOPPED;

/**
 * // TODO (FRa) : (FRa) : comment
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-01
 */
public class ContinuousTrading extends TradingForm<ContinuousTradingState> {

    @Override
    protected Map<ContinuousTradingState, Set<ContinuousTradingState>> getAllowedTransitions() {
        Map<ContinuousTradingState, Set<ContinuousTradingState>>allowedTransitions = new EnumMap<>(ContinuousTradingState.class);
        allowedTransitions.put(STOPPED, singleton(RUNNING));
        allowedTransitions.put(RUNNING, singleton(STOPPED));
        return allowedTransitions;
    }

    @Override
    public ContinuousTradingState getDefaultState() { return STOPPED; }
}
