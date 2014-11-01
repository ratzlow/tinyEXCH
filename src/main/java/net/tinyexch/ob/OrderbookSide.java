package net.tinyexch.ob;

import net.tinyexch.order.Order;
import net.tinyexch.order.OrderType;
import net.tinyexch.order.Trade;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The data structure holding one side (buy or sell) of an order book. An incoming order will be matched against the
 * opposite side.
 * // TODO (FRa) : (FRa) : seggragate
     // stop
     // iceberg
     // hidden
     // non-persistend orders
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-19
 */
public class OrderbookSide {

    private final List<Order> marketOrders = new ArrayList<>();
    private final List<Order> limitOrders = new ArrayList<>();

    public Optional<Trade> match( Order otherSide ) {
        return Optional.empty();
    }

    public void add( Order order ) {

        final OrderType orderType = order.getOrderType();
        switch (orderType) {
            case MARKET:
                marketOrders.add( order );
                break;
            case LIMIT:
                limitOrders.add( order );
                break;
            default:
                String msg = "OrderType '" + order.getOrderType() + "' not considered for orderBook addition!";
                throw new OrderbookException(msg);
        }
    }

    public Collection<Order> getOrders() {
        // TODO (FRa) : (FRa) : replace with jdk8 supplier of unmodifiable list
        return Collections.unmodifiableCollection(
                Stream.concat(marketOrders.stream(), limitOrders.stream()).collect(Collectors.toList())
        );
    }

    public List<Order> getBest( Comparator<Order> byPrios ) {
        List<Order> sortedOrders = new ArrayList<>(limitOrders);
        Collections.sort(sortedOrders, byPrios );
        return Collections.unmodifiableList(sortedOrders);
    }
}
