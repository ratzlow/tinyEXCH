package net.tinyexch.ob.match;

import net.tinyexch.ob.OrderbookSide;
import net.tinyexch.order.Order;

import java.util.Comparator;

/**
 * Knows the applicable matching rules to match against the other side of the book.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-18
 */
@FunctionalInterface
public interface MatchEngine {

    public static final Comparator<Order> SELL_PRICE_ORDERING = Priorities.PRICE;
    public static final Comparator<Order> BUY_PRICE_ORDERING = Priorities.PRICE.reversed();

    public static final Comparator<Order> SELL_STOPPRICE_ORDERING = Priorities.STOP_PRICE;
    public static final Comparator<Order> BUY_STOPPRICE_ORDERING = Priorities.STOP_PRICE.reversed();

    public static final Comparator<Order> SELL_PRICE_TIME_ORDERING = SELL_PRICE_ORDERING.thenComparing(Priorities.TIME);
    public static final Comparator<Order> BUY_PRICE_TIME_ORDERING = BUY_PRICE_ORDERING.thenComparing(Priorities.TIME);

    static final MatchEngine NO_OP = (order, otherOrderbookSide) -> Match.NO_MATCH;

    Match match(Order order, OrderbookSide otherSide);
}
