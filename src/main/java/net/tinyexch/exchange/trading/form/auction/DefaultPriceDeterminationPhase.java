package net.tinyexch.exchange.trading.form.auction;

import net.tinyexch.ob.Orderbook;
import net.tinyexch.ob.match.Priorities;
import net.tinyexch.order.Order;

import java.util.Comparator;
import java.util.List;

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


    //--------------------------------------------------------
    // constructor
    //--------------------------------------------------------

    /**
     * @param orderbook to derive a price for the matchable orders
     */
    public DefaultPriceDeterminationPhase(Orderbook orderbook) {
        this.orderbook = orderbook;
    }

    //--------------------------------------------------------
    // API
    //--------------------------------------------------------

    @Override
    public PriceDeterminationResult determinePrice() {
        List<Order> bidOrders = orderbook.getBuySide().getBest(BUY_PRICE_ORDERING);
        List<Order> askOrders = orderbook.getSellSide().getBest(SELL_PRICE_ORDERING);

        double[] bidPrices = bidOrders.stream().mapToDouble(Order::getPrice).toArray();
        double[] askPrices = askOrders.stream().mapToDouble(Order::getPrice).toArray();

        double askSearchPrice = askOrders.get(0).getPrice();
        double worstMatchableBidPrice = searchClosestBid(askSearchPrice, bidPrices);
        double bidSearchPrice = bidOrders.get(0).getPrice();
        double worstMatchableAskPrice = searchClosestAsk(bidSearchPrice, askPrices);

        int askQty = getMatchableQuantity(askOrders, order -> order.getPrice() <= worstMatchableAskPrice );
        int bidQty = getMatchableQuantity(bidOrders, order -> order.getPrice() >= worstMatchableBidPrice );

        return new PriceDeterminationResult( worstMatchableBidPrice, worstMatchableAskPrice, bidQty, askQty );
    }
}
