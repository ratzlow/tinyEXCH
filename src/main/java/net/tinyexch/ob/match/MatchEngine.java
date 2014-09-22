package net.tinyexch.ob.match;

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

    Optional<Trade> match( Order order );
}
