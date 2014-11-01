package net.tinyexch.exchange.trading.form.auction;

import net.tinyexch.ob.Orderbook;
import net.tinyexch.ob.match.Priorities;
import net.tinyexch.order.Order;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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

        List<Order> bidOrders = orderbook.getBuySide().getBest( BUY_PRICE_ORDERING);
        List<Order> askOrders = orderbook.getSellSide().getBest( SELL_PRICE_ORDERING);

        double[] bidPrices = bidOrders.stream().mapToDouble(Order::getPrice).toArray();
        double[] askPrices = askOrders.stream().mapToDouble(Order::getPrice).toArray();

        double askSearchPrice = askOrders.get(0).getPrice();
        double worstMatchableBidPrice = searchClosestBid(askSearchPrice, bidPrices);
        double bidSearchPrice = bidOrders.get(0).getPrice();
        double worstMatchableAskPrice = searchClosestAsk(bidSearchPrice, askPrices);

        int askQty = getMatchableQuantity(askOrders, order -> order.getPrice() <= worstMatchableAskPrice );
        int bidQty = getMatchableQuantity(bidOrders, order -> order.getPrice() >= worstMatchableBidPrice );
        double auctionPrice = calcAuctionPrice(worstMatchableBidPrice, worstMatchableAskPrice, bidQty, askQty);

        return new PriceDeterminationResult( worstMatchableBidPrice, worstMatchableAskPrice, bidQty, askQty, auctionPrice );
    }


    private double calcAuctionPrice( double bidPrice, double askPrice, int bidQty, int askQty ) {
        final double auctionPrice;
        if ( referencePrice.isPresent() ) {
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

        } else {
            int bidSurplus = PriceDeterminationResult.bidSurplusFunc.apply(bidQty, askQty);
            int askSurplus = PriceDeterminationResult.askSurplusFunc.apply(bidQty, askQty);
            auctionPrice = bidSurplus > askSurplus ? bidPrice : askPrice;
        }

        return auctionPrice;
    }
}
