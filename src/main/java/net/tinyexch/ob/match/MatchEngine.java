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
public interface MatchEngine {

    int NO_PRICE = -1;

    Comparator<Order> SELL_PRICE_ORDERING = Priorities.PRICE.thenComparing(Priorities.HIDDEN_FLAG);
    Comparator<Order> BUY_PRICE_ORDERING = Priorities.PRICE.reversed().thenComparing(Priorities.HIDDEN_FLAG);

    Comparator<Order> SELL_STOPPRICE_ORDERING = Priorities.STOP_PRICE.thenComparing(Priorities.HIDDEN_FLAG);
    Comparator<Order> BUY_STOPPRICE_ORDERING = Priorities.STOP_PRICE.reversed().thenComparing(Priorities.HIDDEN_FLAG);

    Comparator<Order> SELL_PRICE_TIME_ORDERING = SELL_PRICE_ORDERING.thenComparing(Priorities.TIME);
    Comparator<Order> BUY_PRICE_TIME_ORDERING = BUY_PRICE_ORDERING.thenComparing(Priorities.TIME);

    Comparator<Order> VOLUME_TIME_ORDERING = Priorities.VOLUME.reversed().thenComparing(Priorities.TIME);
    Comparator<Order> MIDPOINT_COMPARATOR = VOLUME_TIME_ORDERING.thenComparing(Priorities.SUBMIT_SEQUENCE);


    MatchEngine NO_OP = (order, otherOrderbookSide, thisOrderbookSide ) -> Match.NO_MATCH;

    Match match(Order order, OrderbookSide otherSide, OrderbookSide thisSide);
}
