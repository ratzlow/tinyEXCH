package net.tinyexch.exchange.trading.form.auction;

import net.tinyexch.ob.Orderbook;
import net.tinyexch.ob.OrderbookSide;
import net.tinyexch.ob.match.MatchEngine;
import net.tinyexch.order.Trade;
import net.tinyexch.order.Order;
import net.tinyexch.order.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static net.tinyexch.ob.match.Algos.*;

/**
 * After the call phase (allow sending orders/quotes) of an auction the price is determined while the orderbook is closed.
 * The derived price is the auction price.
 *
 * // TODO (FRa) : (FRa) : integrate into auction
 *
 * @author ratzlow@gmail.com
 * @since 2014-10-06
 */
public class DefaultPriceDeterminationPhase implements PriceDeterminationPhase {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPriceDeterminationPhase.class);


    private static final BiFunction<Order, Double, Boolean> SMO_WORSE_PRICE_BUY_FILTER =
            (Order o, Double auctionPrice) -> o.getOrderType() == OrderType.STRIKE_MATCH && o.getStopPrice() <= auctionPrice;
    private static final BiFunction<Order, Double, Boolean> SMO_WORSE_PRICE_SELL_FILTER =
            (Order o, Double auctionPrice) -> o.getOrderType() == OrderType.STRIKE_MATCH && o.getStopPrice() > auctionPrice;

    //--------------------------------------------------------
    // state
    //--------------------------------------------------------
    private final Orderbook orderbook;
    private final Optional<Double> referencePrice;


    //--------------------------------------------------------
    // constructor
    //--------------------------------------------------------

    /**
     * @param orderbook to derive a price for the matchable orders
     */
    public DefaultPriceDeterminationPhase( Orderbook orderbook ) {
        this.orderbook = orderbook;
        this.referencePrice = Optional.empty();
    }

    public DefaultPriceDeterminationPhase( Orderbook orderbook, Double referencePrice ) {
        this.orderbook = orderbook;
        this.referencePrice = Optional.of(referencePrice);
    }

    //--------------------------------------------------------
    // API
    //--------------------------------------------------------

    @Override
    public PriceDeterminationResult determinePrice() {

        OrderbookSide buySide = orderbook.getBuySide();
        OrderbookSide sellSide = orderbook.getSellSide();

        final PriceDeterminationResult result;
        if ( !buySide.getBest().isEmpty() &&
             !sellSide.getBest().isEmpty() ) {

            LOGGER.info("Orders on both sides available!");
            result = calcResultWithAvailableOrders(buySide, sellSide, referencePrice);

        } else {
            LOGGER.info("No orders available, fallback to use supplied referencePrice as auctionPrice!");
            int bidQty = getMatchableQuantity(new ArrayList<>(buySide.getOrders()), o -> true );
            int askQty = getMatchableQuantity(new ArrayList<>(sellSide.getOrders()), o -> true );

            result = new PriceDeterminationResult(Optional.empty(), Optional.empty(), bidQty, askQty, referencePrice, emptyList());
        }

        Objects.requireNonNull( result, "No price derived from given orderbook and reference price!" );
        return result;
    }

    /**
     * Price determination only considers none-market orders and reference price.
     * Calculation of executable volume will consider market orders as well.
     *
     * @param buySide
     * @param sellSide
     * @param referencePrice if available
     * @return
     */
    private PriceDeterminationResult calcResultWithAvailableOrders(OrderbookSide buySide,
                                                                   OrderbookSide sellSide,
                                                                   Optional<Double> referencePrice) {
        //------------------------------------------------------------------
        // only consider non-MKT orders to find the price range
        //------------------------------------------------------------------

        List<Order> bidOrders = buySide.getBest();
        double[] bidPrices = bidOrders.stream().mapToDouble(Order::getPrice).toArray();
        double bidSearchPrice = bidOrders.isEmpty() ? 0 : bidOrders.get(0).getPrice();

        List<Order> askOrders = sellSide.getBest();
        double[] askPrices = askOrders.stream().mapToDouble(Order::getPrice).toArray();
        double askSearchPrice = askOrders.isEmpty() ? 0 : askOrders.get(0).getPrice();

        Optional<Double> worstMatchableBidPrice = searchClosestBid(askSearchPrice, bidPrices);
        Optional<Double> worstMatchableAskPrice = searchClosestAsk(bidSearchPrice, askPrices);

        //------------------------------------------------------------------
        // to make a statement of matchable quantities also consider MKT
        //------------------------------------------------------------------

        final int askQty;
        if ( worstMatchableAskPrice.isPresent() ) {
            askQty = getMatchableQuantity(sellSide.getOrders(), order -> order.getPrice() <= worstMatchableAskPrice.get() );
        } else { askQty = 0; }

        final int bidQty;
        if ( worstMatchableBidPrice.isPresent() ) {
            bidQty = getMatchableQuantity(buySide.getOrders(), order -> order.getPrice() >= worstMatchableBidPrice.get() );
        } else { bidQty = 0; }

        final Optional<Double> auctionPrice;
        if (worstMatchableAskPrice.isPresent() && worstMatchableBidPrice.isPresent() ) {
            double calcAuctionPrice = calcAuctionPrice(worstMatchableBidPrice.get(), bidQty,
                                                        worstMatchableAskPrice.get(), askQty);
            auctionPrice = Optional.of(calcAuctionPrice);

        // if price cannot be derived from crossing book use limit closest to reference price
        } else if ( referencePrice.isPresent()) {
            auctionPrice = Optional.of( findClosestPriceToReferencePrice( bidSearchPrice, askSearchPrice ) );

        } else {
            auctionPrice = referencePrice;
        }

        //--------------------------------------------------------------------------------------------
        // Depending on the now derived auction price further orders might be applicable for matching
        //--------------------------------------------------------------------------------------------

        int executableBuyQty = calcExecutableBuyQty(buySide.getOrders(), bidQty, auctionPrice, SMO_WORSE_PRICE_BUY_FILTER);
        int executableSellQty = calcExecutableBuyQty(sellSide.getOrders(), askQty, auctionPrice, SMO_WORSE_PRICE_SELL_FILTER);

        //------------------------------------------------------------------
        // Match the orders according to price and executable Qty
        //------------------------------------------------------------------

        List<Order> orderedBuys = new ArrayList<>(buySide.getOrders());
        orderedBuys.sort(MatchEngine.BUY_PRICE_TIME_ORDERING);
        List<Order> orderedSells = new ArrayList<>(sellSide.getOrders());
        orderedSells.sort(MatchEngine.SELL_PRICE_TIME_ORDERING);

        List<Trade> executions = auctionPrice.map( price -> match(orderedBuys, executableBuyQty,
                                                                        orderedSells, executableSellQty,
                                                                        price))
                                                 .orElse(emptyList());

        return new PriceDeterminationResult( worstMatchableBidPrice, worstMatchableAskPrice, executableBuyQty, executableSellQty,
                                             auctionPrice, executions );
    }

    private int calcExecutableBuyQty(Collection<Order> orders, int priceDetermingQty, Optional<Double> auctionPrice, BiFunction<Order, Double, Boolean> filter) {
        final int executableQty;
        if ( auctionPrice.isPresent() ) {
            executableQty = priceDetermingQty + orders.stream().filter( o -> filter.apply(o, auctionPrice.get()))
                    .collect(Collectors.summingInt(Order::getOrderQty));
        } else { executableQty = priceDetermingQty; }

        return executableQty;
    }

    /**
     * @param bestBids according to {@link net.tinyexch.ob.match.Priorities} presorted orders with highest prio first
     * @param bidQtyToMatch bid qty to match from the presorted list
     * @param bestAsks according to {@link net.tinyexch.ob.match.Priorities} presorted orders with highest prio first
     * @param askQtyToMatch ask qty to match from the presorted list
     * @param auctionPrice the execution price for all matches
     * @return the executions of crossing given orders
     */
    private List<Trade> match( List<Order> bestBids, int bidQtyToMatch,
                                   List<Order> bestAsks, int askQtyToMatch,
                                   double auctionPrice ) {

        Queue<Order> bestBidOrders = new LinkedList<>(bestBids);
        Queue<Order> bestAskOrders = new LinkedList<>(bestAsks);
        int alreadyMatchedQty = 0;

        final List<Trade> executions = new ArrayList<>();
        Order bidToMatch = null;
        Order askToMatch = null;
        while ( alreadyMatchedQty < askQtyToMatch && alreadyMatchedQty < bidQtyToMatch &&
                isOpenForExecution(bestBidOrders, bidToMatch) &&
                isOpenForExecution(bestAskOrders, askToMatch)) {

            LOGGER.debug("alreadyMatchedQty={}, askQtyToMatch={}, bidQtyToMatch={}, " +
                         "isOpenForExecutionBid={}, isOpenForExecutionAsk={}",
                    alreadyMatchedQty, askQtyToMatch, bidQtyToMatch,
                    isOpenForExecution(bestBidOrders, bidToMatch),
                    isOpenForExecution(bestAskOrders, askToMatch) );

            if ( bidToMatch == null || leavesQty(bidToMatch) == 0) {
                bidToMatch = bestBidOrders.poll();
            }
            int leaveBidQty = leavesQty(bidToMatch);

            if ( askToMatch == null || leavesQty(askToMatch) == 0 ) {
                askToMatch = bestAskOrders.poll();
            }
            int leaveAskQty = leavesQty(askToMatch);

            int openBidQty = bidQtyToMatch - alreadyMatchedQty;
            int executableBidQty = Math.min(leaveBidQty, openBidQty);

            int openAskQty = askQtyToMatch - alreadyMatchedQty;
            int executableAskQty = Math.min(leaveAskQty, openAskQty);

            int executionQty = Math.min(executableBidQty, executableAskQty);

            bidToMatch = bidToMatch.mutableClone();
            bidToMatch.setCumQty( bidToMatch.getCumQty() + executionQty );

            askToMatch = askToMatch.mutableClone();
            askToMatch.setCumQty( askToMatch.getCumQty() + executionQty );

            Trade execution = Trade.of().setBuy(bidToMatch).setSell(askToMatch)
                                                .setExecutionQty(executionQty).setPrice(auctionPrice);
            executions.add( execution );

            alreadyMatchedQty += executionQty;
        }

        return executions;
    }

    private boolean isOpenForExecution(Queue<Order> orders, Order partialExecuted) {
        return partialExecuted != null || !orders.isEmpty();
    }

    private int leavesQty( Order order ) {
        return order.getOrderQty() - order.getCumQty();
    }

    private double calcAuctionPrice(double bidPrice, int bidQty, double askPrice, int askQty) {
        final double auctionPrice;
        if ( referencePrice.isPresent() ) {
            auctionPrice = findClosestPriceToReferencePrice(bidPrice, askPrice);

        } else {
            int bidSurplus = PriceDeterminationResult.bidSurplusFunc.apply(bidQty, askQty);
            int askSurplus = PriceDeterminationResult.askSurplusFunc.apply(bidQty, askQty);
            auctionPrice = bidSurplus > askSurplus ? bidPrice : askPrice;
        }

        return auctionPrice;
    }

    private double findClosestPriceToReferencePrice(double bidPrice, double askPrice) {
        double auctionPrice;
        Double price = referencePrice.get();
        if ( price.equals(bidPrice)) {
            auctionPrice = bidPrice;

        } else if (price.equals(askPrice)) {
            auctionPrice = askPrice;

        // take closest price to ref price
        } else {
            double bidOffset = Math.abs(price - bidPrice);
            double askOffset = Math.abs(price - askPrice);

            if      (bidOffset < askOffset) auctionPrice = bidPrice;
            else if (askOffset < bidOffset) auctionPrice = askPrice;
            else                            auctionPrice = Math.max(bidPrice, askPrice);
        }
        return auctionPrice;
    }
}
