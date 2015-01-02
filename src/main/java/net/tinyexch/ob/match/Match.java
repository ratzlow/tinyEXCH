package net.tinyexch.ob.match;

import net.tinyexch.order.Order;
import net.tinyexch.order.Trade;

import java.util.Collections;
import java.util.List;
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

    private Match() {
        matchedOrder = null;
        trades = Collections.emptyList();
        state = State.REJECT;
    }

    public Match(Order matchedOrder, List<Trade> trades, State state ) {
        this.matchedOrder = matchedOrder;
        this.trades = trades;
        this.state = state;
    }

    public int getExecutedQuantity() {
        return trades.stream().collect(Collectors.summingInt(Trade::getExecutionQty));
    }

    public Order getMatchedOrder() {
        return matchedOrder;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public State getState() { return state; }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Match{");
        sb.append("matchedOrder=").append(matchedOrder);
        sb.append(", trades=").append(trades);
        sb.append('}');
        return sb.toString();
    }
}
