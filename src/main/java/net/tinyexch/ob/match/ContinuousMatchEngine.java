package net.tinyexch.ob.match;

import net.tinyexch.ob.OrderbookSide;
import net.tinyexch.ob.RejectReason;
import net.tinyexch.ob.match.Match.State;
import net.tinyexch.order.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * Strategy to match new incoming orders against given orderbook.
 *
 * Principle 1: Market orders are given the reference price as a "virtual" price. On this basis,
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
    public Match match(Order order, OrderbookSide otherSide, OrderbookSide thisSide) {
        final List<Trade> trades;
        Order remainingOrder = order;
        State state = State.ACCEPT;
        OrderType orderType = order.getOrderType();
        if ( orderType == OrderType.LIMIT ) {
            trades = matchLimit(order, otherSide, thisSide );

        } else if ( orderType == OrderType.MARKET ) {
            trades = matchMarket( order, otherSide );

        } else if (orderType == OrderType.MARKET_TO_LIMIT ) {
            trades = matchMarketToLimit( order, otherSide );
            if (order.getLeavesQty() > 0 && !trades.isEmpty()) {
                remainingOrder = createRemainingOrder(order, trades);
            }
            state = (trades.size() == 1 && trades.get(0).getExecType() == ExecType.REJECTED) ? State.REJECT : State.ACCEPT;

        } else {
            throw new MatchException("Incoming order has unmatchable order type for continuous trading: " + order);
        }

        return new Match( remainingOrder, trades, state );
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
            Order otherSideOrder = dequeueConditionally(otherSide.getLimitOrders(), leavesQty);
            Trade trade = createTrade( order, otherSideOrder, side, otherSideOrder.getPrice() );

            leavesQty -= trade.getExecutionQty();
            trades.add( trade );
        }

        return trades;
    }


    private List<Trade> matchLimit(Order incomingLimitOrder, OrderbookSide otherSide, OrderbookSide thisSide) {
        final List<Trade> trades = new ArrayList<>();

        int leavesQty = incomingLimitOrder.getLeavesQty();
        while ( leavesQty > 0 && isLiquidityAvailable(otherSide, incomingLimitOrder.getPrice()) ) {
            Side side = otherSide.getSide();
            boolean hasMarketOrders = !otherSide.getMarketOrders().isEmpty();
            boolean hasLimitOrders = !otherSide.getLimitOrders().isEmpty();

            final Trade trade;
            if ( hasMarketOrders && hasLimitOrders ) {
                Order bestLimitOtherSide = otherSide.getLimitOrders().peek();
                Order bestLimitThisSide = thisSide.getLimitOrders().peek();
                double lowestAsk = getBestAskPrice(bestLimitThisSide, bestLimitOtherSide, incomingLimitOrder);
                double highestBid = getBestBidPrice(bestLimitThisSide, bestLimitOtherSide, incomingLimitOrder);
                double executionPrice = calcExecutionPrice( otherSide.getSide(), highestBid, lowestAsk );

                Order otherSideMarketOrder = dequeueConditionally(otherSide.getMarketOrders(), leavesQty);
                trade = createTrade( incomingLimitOrder, otherSideMarketOrder, side, executionPrice );

            } else if ( !hasMarketOrders && hasLimitOrders ) {
                Order otherSideOrder = dequeueConditionally(otherSide.getLimitOrders(), leavesQty);
                trade = createTrade( incomingLimitOrder, otherSideOrder, side, otherSideOrder.getPrice() );

            } else if ( hasMarketOrders && !hasLimitOrders ) {
                Order otherSideOrder = dequeueConditionally(otherSide.getMarketOrders(), leavesQty);
                double executionPrice = calcExecutionPrice(side, incomingLimitOrder.getPrice());
                trade = createTrade( incomingLimitOrder, otherSideOrder, side, executionPrice );

            } else {
                throw new MatchException("Matching not implemented! incomingLimitOrder to match: " + incomingLimitOrder);
            }

            leavesQty -= trade.getExecutionQty();
            trades.add( trade );
        }

        return trades;
    }

    /**
     * Don't remove order from orderbook if it cannot be fully matched, just update it's open Qty.
     *
     * @param otherSideQueue the structure applicable for matching
     * @param incomingOrderLeaveQty the qty to fill for the incoming order
     * @return the orde from other side, which will remain in the book with an open size if it could not be fully
     * matched otherwise it will be removed.
     */
    private Order dequeueConditionally( Queue<Order> otherSideQueue, int incomingOrderLeaveQty ) {
        final Order tradedOrder;
        final Order headOnQueue = otherSideQueue.peek();
        if ( headOnQueue.getLeavesQty() > incomingOrderLeaveQty ) {
            tradedOrder = headOnQueue.mutableClone();
            headOnQueue.setCumQty( headOnQueue.getCumQty() + incomingOrderLeaveQty );
        } else {
            tradedOrder = otherSideQueue.poll();
        }

        return tradedOrder;
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
                Order otherSideOrder = dequeueConditionally(otherSide.getMarketOrders(), leavesQty);
                trade = createTrade( order, otherSideOrder, side, referencePrice );

            } else if ( !hasMarketOrders && hasLimitOrders ) {
                Order otherSideOrder = dequeueConditionally(otherSide.getLimitOrders(), leavesQty);
                trade = createTrade( order, otherSideOrder, side, otherSideOrder.getPrice() );

            } else if ( hasMarketOrders && hasLimitOrders ) {
                Order otherSideOrder = dequeueConditionally(otherSide.getMarketOrders(), leavesQty);
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


    private double calcExecutionPrice(Side otherSide, double highestBidLimit, double lowestAskLimit ) {
        double execPrice = -1;
        if (otherSide == Side.BUY) {
            if (referencePrice >= highestBidLimit && referencePrice >= lowestAskLimit ) {
                execPrice = referencePrice;
            } else if ( highestBidLimit >= lowestAskLimit && highestBidLimit > referencePrice) {
                execPrice = highestBidLimit;
            } else if ( lowestAskLimit > highestBidLimit && lowestAskLimit > referencePrice ) {
                execPrice = lowestAskLimit;
            }
        } else {
            if (referencePrice <= highestBidLimit && referencePrice <= lowestAskLimit ) {
                execPrice = referencePrice;
            } else if (highestBidLimit <= lowestAskLimit && highestBidLimit <= referencePrice) {
                execPrice = highestBidLimit;
            } else if ( lowestAskLimit < highestBidLimit && lowestAskLimit < referencePrice) {
                execPrice = lowestAskLimit;
            }
        }

        if (execPrice == -1) {
            String msg = String.format(
                    "Cannot define execution price for limit order. otherSide=%s, highestBidLimit=%f, lowestAskLimit=%f",
                    otherSide, highestBidLimit, lowestAskLimit);
            throw new MatchException(msg);
        }

        return execPrice;
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

    /**
     * @param otherSide orderbook side with orders to match against
     * @param limitPrice of this side order to check if we are in the market (against the other side best limit)
     * @return true ... order can be crossed with order on the other side
     */
    private boolean isLiquidityAvailable(OrderbookSide otherSide, double limitPrice ) {

        boolean hasExecutableLimitOrders = false;
        if (!otherSide.getLimitOrders().isEmpty()) {
            Order limitOnOtherSide = otherSide.getLimitOrders().peek();
            hasExecutableLimitOrders = otherSide.getSide() == Side.BUY ?
                                            isCrossedPrice(limitOnOtherSide.getPrice(), limitPrice) :
                                            isCrossedPrice(limitPrice, limitOnOtherSide.getPrice());
        }

        return !otherSide.getMarketOrders().isEmpty() ||
                hasExecutableLimitOrders ||
                !otherSide.getHiddenOrders().isEmpty() ||
                !otherSide.getHiddenOrders().isEmpty();
    }

    private boolean isCrossedPrice( double bid, double ask ) {
        return bid >= ask;
    }

    private double getBestAskPrice( Order bestThisSide, Order bestOtherSide, Order incoming ) {
        return getBestPrice(bestThisSide, bestOtherSide, incoming, Side.SELL, SELL_PRICE_ORDERING);
    }

    private double getBestBidPrice( Order bestThisSide, Order bestOtherSide, Order incoming ) {
        return getBestPrice(bestThisSide, bestOtherSide, incoming, Side.BUY, BUY_PRICE_ORDERING);
    }

    private double getBestPrice(Order bestThisSide, Order bestOtherSide, Order incoming, Side side, Comparator<Order> bestFirst ) {
        return Stream.of(bestThisSide, bestOtherSide, incoming)
                .filter( o -> o != null ).filter( o -> o.getSide() == side)
                .sorted(bestFirst).findFirst().get().getPrice();
    }
}