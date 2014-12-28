package net.tinyexch.ob.match;

import net.tinyexch.ob.OrderbookSide;
import net.tinyexch.order.Order;
import net.tinyexch.order.OrderType;
import net.tinyexch.order.Side;
import net.tinyexch.order.Trade;

import java.util.*;

/**
 * Strategy to match new incoming orders against given orderbook.
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-23
 */
// TODO (FRa) : (FRa) : consider TimeInForce for partial matching
public class ContinuousMatchEngine implements MatchEngine {
    private final double referencePrice;

    public ContinuousMatchEngine( double referencePrice) {
        this.referencePrice = referencePrice;
    }

    @Override
    public Match match(Order order, OrderbookSide otherSide) {
        final List<Trade> trades;
        OrderType orderType = order.getOrderType();
        if ( orderType == OrderType.MARKET ) {
            trades = matchMarket( order, otherSide );
        } else {
            trades = Collections.emptyList();
        }

        return new Match(order, trades);
    }


    private List<Trade> matchMarket(Order order, OrderbookSide otherSide) {
        List<Trade> trades = new ArrayList<>();

        int leavesQty = order.getLeavesQty();
        while ( leavesQty > 0 && isLiquidityAvailable(otherSide) ) {
            Trade trade;
            Side side = otherSide.getSide();
            if ( !otherSide.getMarketOrders().isEmpty() ) {
                Order otherSideOrder = otherSide.getMarketOrders().poll();
                trade = createTrade( order, otherSideOrder, side, referencePrice );

            } else if ( !otherSide.getLimitOrders().isEmpty() ) {
                Order otherSideOrder = otherSide.getLimitOrders().poll();
                trade = createTrade( order, otherSideOrder, side, otherSideOrder.getPrice() );

            } else {
                throw new UnsupportedOperationException("Matching not implemented! order to match: " + order);
            }

            leavesQty -= trade.getExecutionQty();
            trades.add( trade );
        }

        return trades;
    }


    private Trade createTrade(Order order, Order otherSideOrder, Side otherSide, double price ) {
        Order buy = otherSide == Side.BUY ? otherSideOrder.mutableClone() : order.mutableClone();
        Order sell = otherSide == Side.SELL ? otherSideOrder.mutableClone() : order.mutableClone();

        int takeQty = Math.min(buy.getLeavesQty(), sell.getLeavesQty());
        buy.setCumQty( buy.getCumQty() + takeQty );
        sell.setCumQty( sell.getCumQty() + takeQty );

        return Trade.of().setBuy(buy).setSell(sell).setExecutionQty(takeQty).setPrice(price);
    }

    /**
     * @param otherSide all orders by their type on the other side
     * @return true ... there is open liquidity on the other side that might be used for matching
     */
    private boolean isLiquidityAvailable(OrderbookSide otherSide) {
        return !otherSide.getMarketOrders().isEmpty() ||
                !otherSide.getLimitOrders().isEmpty() ||
                !otherSide.getHiddenOrders().isEmpty() ||
                !otherSide.getHiddenOrders().isEmpty();
    }
}