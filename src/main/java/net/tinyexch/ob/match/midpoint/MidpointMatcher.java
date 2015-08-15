package net.tinyexch.ob.match.midpoint;

import net.tinyexch.ob.OrderbookSide;
import net.tinyexch.ob.match.MatchCollector;
import net.tinyexch.ob.match.TradeFactory;
import net.tinyexch.ob.price.safeguard.VolatilityInterruptionGuard;
import net.tinyexch.order.Order;
import net.tinyexch.order.Side;
import net.tinyexch.order.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static net.tinyexch.ob.match.TradeFactory.isCrossedPrice;

/**
 * Strategy to match incoming midpoint order against midpoint orders on the other side. Does some backtracking and
 * steals already executed Qty to maximize executable volume.
 *
 * @author ratzlow@gmail.com
 * @since 2015-07-21
 */
public class MidpointMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MidpointMatcher.class);
    private static final MatcherSupport MATCHER = MatcherSupport.SELF;
    private final double midpointPrice;
    private final VolatilityInterruptionGuard priceGuard;


    //-------------------------------------------------------------------------------------
    // constructors
    //-------------------------------------------------------------------------------------

    public MidpointMatcher(double midpointPrice, VolatilityInterruptionGuard priceGuard ) {
        this.midpointPrice = midpointPrice;
        this.priceGuard = priceGuard;
    }

    //-------------------------------------------------------------------------------------
    // public API
    //-------------------------------------------------------------------------------------

    public MatchCollector matchMidpoint( Order incoming, OrderbookSide otherSide ) {
        QtyCollector qtyCollector = MATCHER.collectQty(incoming, otherSide);

        MatchCollector collector = new MatchCollector();
        Map<String, ExecutionChance> executableOrders = qtyCollector.getPotentialMatches().stream().collect(
                toMap(chance -> chance.getOtherSide().getClientOrderID(), Function.identity())
        );

        Collection<Order> keepInBook = new ArrayList<>();
        Queue<Order> otherSideOrders = otherSide.getLimitOrders();
        while ( !executableOrders.isEmpty() && !otherSideOrders.isEmpty() ) {
            Order head = otherSideOrders.poll();
            LOGGER.debug("Other side order from book orderID={}", head.getClientOrderID());
            Optional<ExecutionChance> executedOrder = Optional.ofNullable(executableOrders.get(head.getClientOrderID()))
                                                                .map(Function.identity());
            if ( executedOrder.isPresent() ) {
                Order otherSideOrder = executedOrder.get().getOtherSide();
                if ( isGoodPrice(incoming, otherSideOrder) ) {

                    Trade trade = TradeFactory.createTrade(
                            incoming, otherSideOrder, midpointPrice, (buy, sell) -> executedOrder.get().executableQty
                    );
                    collector.getTrades().add(trade);
                    Order updatedOtherSideOrder = findBySide( trade, otherSideOrder.getSide() );
                    Order updatedThisSideOrder = findBySide( trade, incoming.getSide() );
                    // update executed size
                    incoming.setCumQty( updatedThisSideOrder.getCumQty() );

                    if ( updatedOtherSideOrder.getLeavesQty() > 0 ) {
                        keepInBook.add( updatedOtherSideOrder );
                    }
                } else {
                    keepInBook.add( head );
                }

                LOGGER.debug("Removed match for clientOrderID={}", otherSideOrder.getClientOrderID());
                executableOrders.remove(otherSideOrder.getClientOrderID());
            }
        }

        // add all open orders from other side back to book
        keepInBook.stream().forEach(otherSide::add);

        return collector;
    }


    // TODO (FRa) : (FRa) : price check should be added at finding match chance
    private boolean isGoodPrice(Order incomingOrder, Order otherSideOrder) {
        double otherSidePrice = otherSideOrder.getPrice();
        Side side = incomingOrder.getSide();
        double bid = side == Side.BUY ? incomingOrder.getPrice() : otherSidePrice;
        double ask = side == Side.SELL ? incomingOrder.getPrice() : otherSidePrice;
        boolean betterThanMidpointPrice = bid >= midpointPrice && ask <= midpointPrice;
        boolean midpointPriceInPriceRanges = !priceGuard.checkIndicativePrice(midpointPrice).isPresent();

        return betterThanMidpointPrice && midpointPriceInPriceRanges && isCrossedPrice(bid, ask);
    }

    private Order findBySide(Trade trade, Side side) {
        return trade.getBuy().getSide() == side ? trade.getBuy() : trade.getSell();
    }
}
