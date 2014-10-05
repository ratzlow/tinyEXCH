package net.tinyexch.ob.match;

import net.tinyexch.ob.OrderbookSide;
import net.tinyexch.order.Order;
import net.tinyexch.order.Trade;

import java.util.Optional;

/**
 * Knows the applicable matching rules to match against the other side of the book.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-18
 */
@FunctionalInterface
public interface MatchEngine {

    static final MatchEngine NO_OP = (order, otherOrderbookSide) -> Optional.empty();

    Optional<Trade> match(Order order, OrderbookSide toMatchAgainst);
}
