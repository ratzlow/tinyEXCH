package net.tinyexch.exchange.trading.form.auction;

import net.tinyexch.ob.Orderbook;
import net.tinyexch.ob.match.Priorities;
import net.tinyexch.order.Execution;
import net.tinyexch.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    public static final Comparator<Order> SELL_PRICE_TIME_ORDERING = Priorities.PRICE.thenComparing(Priorities.TIME);
    public static final Comparator<Order> BUY_PRICE_TIME_ORDERING = Priorities.PRICE.reversed().thenComparing(Priorities.TIME);

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
    public DefaultPriceDeterminationPhase(Orderbook orderbook) {
        this.orderbook = orderbook;
        this.referencePrice = Optional.empty();
    }

    public DefaultPriceDeterminationPhase(Orderbook orderbook, Double referencePrice ) {
        this.orderbook = orderbook;
        this.referencePrice = Optional.of(referencePrice);
    }


    //--------------------------------------------------------
    // API
    //--------------------------------------------------------

    @Override
    public PriceDeterminationResult determinePrice() {

        List<Order> bidOrders = orderbook.getBuySide().getBest(BUY_PRICE_TIME_ORDERING);
        List<Order> askOrders = orderbook.getSellSide().getBest(SELL_PRICE_TIME_ORDERING);

        PriceDeterminationResult result = null;
        if ( !bidOrders.isEmpty() && !askOrders.isEmpty() ) {
            LOGGER.info("Orders on both sides available!");
            result = calcResultWithAvailableOrders(bidOrders, askOrders, referencePrice);

        } else {
            LOGGER.info("No orders available, fallback to use supplied referencePrice as auctionPrice!");
            int bidQty = getMatchableQuantity(new ArrayList<>(orderbook.getBuySide().getOrders()), o -> true );
            int askQty = getMatchableQuantity(new ArrayList<>(orderbook.getSellSide().getOrders()), o -> true );

            result = new PriceDeterminationResult(Optional.empty(), Optional.empty(), bidQty, askQty, referencePrice, emptyList());
        }

        Objects.requireNonNull( result, "No price derived from given orderbook and reference price!" );
        return result;
    }

    private PriceDeterminationResult calcResultWithAvailableOrders(List<Order> bidOrders, List<Order> askOrders,
                                                                   Optional<Double> referencePrice) {
        double[] bidPrices = bidOrders.stream().mapToDouble(Order::getPrice).toArray();
        double bidSearchPrice = bidOrders.isEmpty() ? 0 : bidOrders.get(0).getPrice();

        double[] askPrices = askOrders.stream().mapToDouble(Order::getPrice).toArray();
        double askSearchPrice = askOrders.isEmpty() ? 0 : askOrders.get(0).getPrice();

        Optional<Double> worstMatchableBidPrice = searchClosestBid(askSearchPrice, bidPrices);
        Optional<Double> worstMatchableAskPrice = searchClosestAsk(bidSearchPrice, askPrices);

        final int askQty;
        if ( worstMatchableAskPrice.isPresent() ) {
            askQty = getMatchableQuantity(askOrders, order -> order.getPrice() <= worstMatchableAskPrice.get() );
        } else { askQty = 0; }

        final int bidQty;
        if ( worstMatchableBidPrice.isPresent() ) {
            bidQty = getMatchableQuantity(bidOrders, order -> order.getPrice() >= worstMatchableBidPrice.get() );
        } else { bidQty = 0; }

        Optional<Double> auctionPrice = referencePrice;
        if (worstMatchableAskPrice.isPresent() && worstMatchableBidPrice.isPresent() ) {
            double calcAuctionPrice = calcAuctionPrice(worstMatchableBidPrice.get(), worstMatchableAskPrice.get(), bidQty, askQty);
            auctionPrice = Optional.of(calcAuctionPrice);

        // if price cannot be derived from crossing book use limit closest to reference price
        } else if ( referencePrice.isPresent()) {
            auctionPrice = Optional.of( findClosestPriceToReferencePrice( bidSearchPrice, askSearchPrice ) );
        }

        List<Execution> executions = auctionPrice.map( price -> match(bidOrders, bidQty, askOrders, askQty, price)).orElse(emptyList());

        return new PriceDeterminationResult( worstMatchableBidPrice, worstMatchableAskPrice, bidQty, askQty,
                                             auctionPrice, executions );
    }

    /**
     * @param bestBids according to {@link net.tinyexch.ob.match.Priorities} presorted orders with highest prio first
     * @param bidQtyToMatch bid qty to match from the presorted list
     * @param bestAsks according to {@link net.tinyexch.ob.match.Priorities} presorted orders with highest prio first
     * @param askQtyToMatch ask qty to match from the presorted list
     * @param auctionPrice the execution price for all matches
     * @return the executions of crossing given orders
     */
    private List<Execution> match( List<Order> bestBids, int bidQtyToMatch,
                                   List<Order> bestAsks, int askQtyToMatch,
                                   double auctionPrice ) {

        Queue<Order> bestBidOrders = new LinkedList<>(bestBids);
        Queue<Order> bestAskOrders = new LinkedList<>(bestAsks);
        int alreadyMatchedQty = 0;

        final List<Execution> executions = new ArrayList<>();
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

            Execution execution = Execution.of().setBuy(bidToMatch).setSell(askToMatch)
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

    private double calcAuctionPrice( double bidPrice, double askPrice, int bidQty, int askQty ) {
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
