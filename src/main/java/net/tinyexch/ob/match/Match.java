package net.tinyexch.ob.match;

import net.tinyexch.ob.price.safeguard.VolatilityInterruption;
import net.tinyexch.order.Order;
import net.tinyexch.order.Trade;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The result of all matches for given Order.
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-25
 */
public class Match {
    public enum State { ACCEPT, REJECT }

    public static final Match NO_MATCH = new Match();

    private final Order matchedOrder;
    private final List<Trade> trades;
    private final State state;
    private final Optional<VolatilityInterruption> volatilityInterruption;

    private Match() {
        matchedOrder = null;
        trades = Collections.emptyList();
        state = State.REJECT;
        volatilityInterruption = Optional.empty();
    }

    public Match(Order matchedOrder, List<Trade> trades, State state, Optional<VolatilityInterruption> volatilityInterruption ) {
        this.matchedOrder = matchedOrder;
        this.trades = trades;
        this.state = state;
        this.volatilityInterruption = volatilityInterruption;
    }

    public int getExecutedQuantity() {
        return trades.stream().collect(Collectors.summingInt(Trade::getExecutionQty));
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public State getState() { return state; }

    public Optional<VolatilityInterruption> getVolatilityInterruption() {
        return volatilityInterruption;
    }

    @Override
    public String toString() {
        return "Match{" +
                "matchedOrder=" + matchedOrder +
                ", trades=" + trades +
                ", state=" + state +
                ", volatilityInterruption=" + volatilityInterruption +
                '}';
    }
}
