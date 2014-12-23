package net.tinyexch.ob.match;

import net.tinyexch.ob.OrderbookSide;
import net.tinyexch.order.Order;
import net.tinyexch.order.Trade;

import java.util.Optional;

/**
 * Strategy to match new incoming orders against given orderbook.
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-23
 */
public class ContinuousMatchEngine implements MatchEngine {
    private final double referencePrice;

    public ContinuousMatchEngine( double referencePrice) {
        this.referencePrice = referencePrice;
    }

    @Override
    public Optional<Trade> match( Order order, OrderbookSide otherSide ) {
        return Optional.empty();
    }
}
