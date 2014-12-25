package net.tinyexch.ob;

import net.tinyexch.order.Order;
import net.tinyexch.order.OrderType;
import net.tinyexch.order.Trade;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * The data structure holding one side (buy or sell) of an order book. An incoming order will be matched against the
 * opposite side. Main priority for matching is derived by the {@link net.tinyexch.order.OrderType}
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

    private final Queue<Order> marketOrders;
    private final Queue<Order> limitOrders;
    private final Queue<Order> hiddenOrders;
    private final Queue<Order> strikeMatchOrders;
    private final Comparator<Order> priceTimeOrdering;

    //-----------------------------------------------------------------------------------------------
    // constructors
    //-----------------------------------------------------------------------------------------------


    public OrderbookSide( Comparator<Order> byPrice, Comparator<Order> byTime, Comparator<Order> byTriggerPrice ) {
        priceTimeOrdering = byPrice.thenComparing(byTime);
        marketOrders = new PriorityQueue<>(byTime);
        limitOrders = new PriorityQueue<>(priceTimeOrdering);
        hiddenOrders = new PriorityQueue<>(priceTimeOrdering);
        strikeMatchOrders = new PriorityQueue<>( byTriggerPrice);
    }


    //-----------------------------------------------------------------------------------------------
    // public API
    //-----------------------------------------------------------------------------------------------

    public Optional<Trade> match( Order otherSide ) {
        return Optional.empty();
    }

    public void add( Order order ) {

        final OrderType orderType = order.getOrderType();
        switch (orderType) {
            case STRIKE_MATCH:
                strikeMatchOrders.add( order );
                break;
            case MARKET:
                marketOrders.add( order );
                break;
            case LIMIT:
                limitOrders.add( order );
                break;
            case HIDDEN:
                hiddenOrders.add( order );
                break;
            default:
                String msg = "OrderType '" + order.getOrderType() + "' not considered for orderBook addition!";
                throw new OrderbookException(msg);
        }
    }

    public Collection<Order> getOrders() {
        return Collections.unmodifiableCollection(Stream.of(marketOrders, limitOrders, hiddenOrders, strikeMatchOrders)
                .flatMap(Collection::stream).collect(toList()));
    }

    /**
     * Orders to consider if price needs to be derived.
     *
     * @return orders where best is on top of the book
     */
    public List<Order> getBest() {
        List<Order> sortedOrders = Stream.of(limitOrders, hiddenOrders)
                                         .flatMap(Collection::stream).collect(toList());
        Collections.sort( sortedOrders, this.priceTimeOrdering );
        return Collections.unmodifiableList(sortedOrders);
    }
}
