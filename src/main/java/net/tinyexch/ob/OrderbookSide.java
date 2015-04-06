package net.tinyexch.ob;

import net.tinyexch.ob.match.Priorities;
import net.tinyexch.order.Order;
import net.tinyexch.order.OrderType;
import net.tinyexch.order.Side;

import java.util.*;

import static java.util.Collections.unmodifiableCollection;
import static java.util.stream.Collectors.toList;

/**
 * The data structure holding one side (buy or sell) of an order book. An incoming order will be matched against the
 * opposite side. Main priority for matching is derived by the {@link net.tinyexch.order.OrderType}
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-19
 */
public class OrderbookSide {

    private final Map<OrderType, Queue<Order>> ordersByType;
    private final Comparator<Order> priceTimeOrdering;
    private final Side side;

    //-----------------------------------------------------------------------------------------------
    // constructors
    //-----------------------------------------------------------------------------------------------

    public OrderbookSide( Side side, Comparator<Order> byPriceThenTimeOrdering, Comparator<Order> byTriggerPrice ) {
        this.side = side;
        this.priceTimeOrdering = byPriceThenTimeOrdering.thenComparing(Priorities.SUBMIT_SEQUENCE);
        this.ordersByType = new EnumMap<>(OrderType.class);

        ordersByType.put(OrderType.STRIKE_MATCH, new PriorityQueue<>(byTriggerPrice.thenComparing(Priorities.SUBMIT_SEQUENCE)) );
        ordersByType.put(OrderType.MARKET, new PriorityQueue<>(Priorities.TIME.thenComparing(Priorities.SUBMIT_SEQUENCE)) );
        ordersByType.put(OrderType.LIMIT, new PriorityQueue<>(priceTimeOrdering) );
    }


    //-----------------------------------------------------------------------------------------------
    // public API
    //-----------------------------------------------------------------------------------------------

    public void add( Order order ) {
        OrderType orderType = order.getOrderType();
        Queue<Order> orders = ordersByType.get(orderType);
        if ( orders != null ) {
            orders.offer( order );
        } else {
            String msg = "OrderType '" + order.getOrderType() + "' not considered for orderBook addition!";
            throw new OrderbookException(msg);
        }
    }

    public void cancel( Order order ) {
        Queue<Order> orders = ordersByType.get(order.getOrderType());
        orders.removeIf( o -> o.getClientOrderID().equals(order.getClientOrderID()) );
    }


    public Collection<Order> getOrders() {
        return unmodifiableCollection(ordersByType.values().stream().flatMap(Collection::stream).collect(toList()));
    }

    /**
     * Orders to consider if price needs to be derived. Hidden orders are ignored!
     *
     * @return orders where best is on top of the book
     */
    public List<Order> getBest() {
        List<Order> sortedOrders = new ArrayList<>(
                ordersByType.get(OrderType.LIMIT).stream().filter(o -> !o.isHidden()).collect(toList())
        );
        Collections.sort( sortedOrders, this.priceTimeOrdering );
        return Collections.unmodifiableList(sortedOrders);
    }

    public Queue<Order> getMarketOrders() {
        return ordersByType.get(OrderType.MARKET);
    }

    public Queue<Order> getLimitOrders() {
        return ordersByType.get(OrderType.LIMIT);
    }

    public Side getSide() { return side; }

    /**
     * @return true ... there is open liquidity on the other side that might be used for matching
     */
    public boolean isLiquidityAvailable() {
        return ordersByType.values().stream().flatMap(Collection::stream).findAny().isPresent();
    }
}