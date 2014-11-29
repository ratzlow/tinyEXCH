package net.tinyexch.exchange.trading.form.auction;

import net.tinyexch.ob.Orderbook;
import net.tinyexch.ob.match.Priorities;
import net.tinyexch.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    public static final Comparator<Order> SELL_PRICE_ORDERING = Priorities.PRICE;
    public static final Comparator<Order> BUY_PRICE_ORDERING = Priorities.PRICE.reversed();

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

        List<Order> bidOrders = orderbook.getBuySide().getBest(BUY_PRICE_ORDERING);
        List<Order> askOrders = orderbook.getSellSide().getBest(SELL_PRICE_ORDERING);

        PriceDeterminationResult result = null;
        if ( !bidOrders.isEmpty() && !askOrders.isEmpty() ) {
            LOGGER.info("Orders on both sides available!");
            result = calcResultWithAvailableOrders(bidOrders, askOrders, referencePrice);

        } else if ( bidOrders.isEmpty() && !askOrders.isEmpty() ) {
            // TODO (FRa) : (FRa) : implement

        } else if ( !bidOrders.isEmpty() && askOrders.isEmpty() ) {
            // TODO (FRa) : (FRa) : implement

        } else {
            LOGGER.info("No orders available, fallback to use supplied referencePrice as auctionPrice!");
            int bidQty = getMatchableQuantity(new ArrayList<>(orderbook.getBuySide().getOrders()), o -> true );
            int askQty = getMatchableQuantity(new ArrayList<>(orderbook.getSellSide().getOrders()), o -> true );

            result = new PriceDeterminationResult(Optional.empty(), Optional.empty(), bidQty, askQty, referencePrice);
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

        int askQty = 0;
        if ( worstMatchableAskPrice.isPresent() ) {
            askQty = getMatchableQuantity(askOrders, order -> order.getPrice() <= worstMatchableAskPrice.get() );
        }

        int bidQty = 0;
        if ( worstMatchableBidPrice.isPresent() ) {
            bidQty = getMatchableQuantity(bidOrders, order -> order.getPrice() >= worstMatchableBidPrice.get() );
        }

        Optional<Double> auctionPrice = referencePrice;
        if (worstMatchableAskPrice.isPresent() && worstMatchableBidPrice.isPresent() ) {
            double calcAuctionPrice = calcAuctionPrice(worstMatchableBidPrice.get(), worstMatchableAskPrice.get(), bidQty, askQty);
            auctionPrice = Optional.of(calcAuctionPrice);

        // if price cannot be derived from crossing book use limit closest to reference price
        } else if ( referencePrice.isPresent()) {
            auctionPrice = Optional.of( findClosestPriceToReferencePrice( bidSearchPrice, askSearchPrice ) );
        }

        return new PriceDeterminationResult( worstMatchableBidPrice, worstMatchableAskPrice, bidQty, askQty, auctionPrice );
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
