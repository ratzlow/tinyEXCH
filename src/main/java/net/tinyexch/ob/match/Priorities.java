package net.tinyexch.ob.match;

import net.tinyexch.order.Order;

import java.util.Comparator;

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
     * Highest price first
     */
    PRICE {
        @Override
        public int compare(Order o1, Order o2) { return Double.compare(o1.getPrice(), o2.getPrice()); }
    }
}
