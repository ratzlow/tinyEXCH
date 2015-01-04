package net.tinyexch.ob.match;

import net.tinyexch.ob.OrderbookSide;
import net.tinyexch.ob.RejectReason;
import net.tinyexch.ob.match.Match.State;
import net.tinyexch.order.*;

import java.util.*;

/**
 * Strategy to match new incoming orders against given orderbook.
 *
 * Principle 1: Market orders are given the reference price as a ”virtual” price. On this basis,
 * execution is carried out at the reference price provided that this does not violate price/time priority.
 *
 * Principle 2: If orders cannot be executed at the reference price, they are executed in accordance with price/time
 * priority by means of price determination above or below the reference price (non-executed bid market orders or
 * ask market orders) i.e. the price is determined by a limit within the order book or a limit of an incoming order.
 *
 * @author ratzlow@gmail.com
 * @since 2014-12-23
 */
// TODO (FRa) : (FRa) : consider TimeInForce for partial matching (e.g FOK, ...)
public class ContinuousMatchEngine implements MatchEngine {
    private final double referencePrice;

    public ContinuousMatchEngine( double referencePrice) {
        this.referencePrice = referencePrice;
    }

    @Override
    public Match match(Order order, OrderbookSide otherSide) {
        final List<Trade> trades;
        Order remainingOrder = order;
        State state = State.ACCEPT;
        OrderType orderType = order.getOrderType();
        if ( orderType == OrderType.MARKET ) {
            trades = matchMarket( order, otherSide );

        } else if (orderType == OrderType.MARKET_TO_LIMIT ) {
            trades = matchMarketToLimit( order, otherSide );
            if (order.getLeavesQty() > 0 && !trades.isEmpty()) {
                remainingOrder = createRemainingOrder(order, trades);
            }
            state = (trades.size() == 1 && trades.get(0).getExecType() == ExecType.REJECTED) ? State.REJECT : State.ACCEPT;

        } else {
            trades = Collections.emptyList();
        }

        return new Match(remainingOrder, trades, state );
    }

    private Order createRemainingOrder(Order unexecutedOrderPart, List<Trade> trades) {
        Trade firstTrade = trades.get(0);
        double newApplicableLimitPrice = firstTrade.getPrice();
        return unexecutedOrderPart.setOrderType(OrderType.LIMIT).setPrice(newApplicableLimitPrice);
    }


    private List<Trade> matchMarketToLimit(Order order, OrderbookSide otherSide) {
        final List<Trade> trades;
        boolean hasLimitOrdersOnly = !otherSide.getLimitOrders().isEmpty() && otherSide.getMarketOrders().isEmpty();
        if ( hasLimitOrdersOnly ) {
            trades = matchAgainstLimitOrders(order, otherSide );

        } else {
            final Order buy, sell;
            if (order.getSide() == Side.BUY) {
                buy = order;
                sell = null;
            } else {
                buy = null;
                sell = order;
            }

            Trade trade = Trade.of().setBuy(buy).setSell(sell).setExecType(ExecType.REJECTED)
                    .setOrderRejectReason(RejectReason.INSUFFICIENT_OB_CONSTELLATION.getMsg());
            trades = Collections.singletonList( trade );
        }

        return trades;
    }


    private List<Trade> matchAgainstLimitOrders( Order order, OrderbookSide otherSide ) {
        final List<Trade> trades = new ArrayList<>();

        int leavesQty = order.getLeavesQty();
        while ( leavesQty > 0 && !otherSide.getLimitOrders().isEmpty() ) {
            Side side = otherSide.getSide();
            Order otherSideOrder = otherSide.getLimitOrders().poll();
            Trade trade = createTrade( order, otherSideOrder, side, otherSideOrder.getPrice() );

            leavesQty -= trade.getExecutionQty();
            trades.add( trade );
        }

        return trades;
    }


    private List<Trade> matchMarket(Order order, OrderbookSide otherSide) {
        final List<Trade> trades = new ArrayList<>();

        int leavesQty = order.getLeavesQty();
        while ( leavesQty > 0 && isLiquidityAvailable(otherSide) ) {
            Side side = otherSide.getSide();
            boolean hasMarketOrders = !otherSide.getMarketOrders().isEmpty();
            boolean hasLimitOrders = !otherSide.getLimitOrders().isEmpty();
            Trade trade;
            if ( hasMarketOrders && !hasLimitOrders ) {
                Order otherSideOrder = otherSide.getMarketOrders().poll();
                trade = createTrade( order, otherSideOrder, side, referencePrice );

            } else if ( !hasMarketOrders && hasLimitOrders ) {
                Order otherSideOrder = otherSide.getLimitOrders().poll();
                trade = createTrade( order, otherSideOrder, side, otherSideOrder.getPrice() );

            } else if ( hasMarketOrders && hasLimitOrders ) {
                Order otherSideOrder = otherSide.getMarketOrders().poll();
                double bestPriceOnOtherSide = otherSide.getLimitOrders().peek().getPrice();
                double executionPrice = calcExecutionPrice(side, bestPriceOnOtherSide);
                trade = createTrade( order, otherSideOrder, side, executionPrice );

            } else {
                throw new UnsupportedOperationException("Matching not implemented! order to match: " + order);
            }

            leavesQty -= trade.getExecutionQty();
            trades.add( trade );
        }

        return trades;
    }


    private double calcExecutionPrice(Side otherSide, double bestPriceOnOtherSide ) {
        final double executionPrice;
        if (otherSide == Side.BUY) {
            executionPrice = referencePrice >= bestPriceOnOtherSide ? referencePrice : bestPriceOnOtherSide;
        } else {
            executionPrice = referencePrice <= bestPriceOnOtherSide ? referencePrice : bestPriceOnOtherSide;
        }

        return executionPrice;
    }


    private Trade createTrade(Order order, Order otherSideOrder, Side otherSide, double price ) {
        Order buy = otherSide == Side.BUY ? otherSideOrder.mutableClone() : order.mutableClone();
        Order sell = otherSide == Side.SELL ? otherSideOrder.mutableClone() : order.mutableClone();

        int takeQty = Math.min(buy.getLeavesQty(), sell.getLeavesQty());
        buy.setCumQty( buy.getCumQty() + takeQty );
        sell.setCumQty( sell.getCumQty() + takeQty );

        return Trade.of().setBuy(buy).setSell(sell).setExecutionQty(takeQty).setPrice(price).setExecType(ExecType.TRADE);
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