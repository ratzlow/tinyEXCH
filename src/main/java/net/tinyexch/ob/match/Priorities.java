package net.tinyexch.ob.match;

import net.tinyexch.order.Order;
import net.tinyexch.order.OrderType;

import java.util.Comparator;

import static net.tinyexch.order.OrderType.MARKET;

/**
 * Matching is done along descending priorities. The ordering of priorities depends on the actual market.
 *
 * @author ratzlow@gmail.com
 * @since 2014-10-05
 */
public enum Priorities implements Comparator<Order> {

    /**
     * Earlier order always has precedence.
     */
    TIME {
        @Override
        public int compare(Order o1, Order o2) {
            return o1.getTimestamp().compareTo(o2.getTimestamp());
        }
    },

    /**
     * Lowest price first. Ensure market orders are always best price so they come before any price restricted order.
     */
    PRICE {
        private Comparator<Order> MARKET_ALWAYS_BEST = (o1, o2) -> {
            OrderType leftType = o1.getOrderType();
            OrderType rightType = o2.getOrderType();
            if      (leftType == MARKET && rightType != MARKET) return -1;
            else if (leftType != MARKET && rightType == MARKET) return 1;
            else                                                return 0;
        };

        private Comparator<Order> BY_PRICE = (o1, o2) -> Double.compare(o1.getPrice(), o2.getPrice());

        @Override
        public int compare(Order o1, Order o2) {
            return MARKET_ALWAYS_BEST.thenComparing( BY_PRICE ).compare(o1, o2);
        }

        @Override
        public Comparator<Order> reversed() {
            return MARKET_ALWAYS_BEST.thenComparing( BY_PRICE.reversed());
        }
    }
}
