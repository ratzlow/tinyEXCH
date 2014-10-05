package net.tinyexch.ob;

import net.tinyexch.order.Order;
import net.tinyexch.order.Trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The data structure holding one side (buy or sell) of an order book. An incoming order will be matched against the
 * opposite side.
 * // TODO (FRa) : (FRa) : seggragate
     // market orders
     // limit
     // stop
     // iceberg
     // hidden
     // non-persistend orders
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-19
 */
public class OrderbookSide {

    private final List<Order> orders = new ArrayList<>();

    public Optional<Trade> match( Order otherSide ) {
        return Optional.empty();
    }

    public void add( Order thisSideOrder ) {
        orders.add( thisSideOrder );
    }
}
