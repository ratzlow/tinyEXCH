package net.tinyexch.ob.match;

import net.tinyexch.order.ExecType;
import net.tinyexch.order.Order;
import net.tinyexch.order.Side;
import net.tinyexch.order.Trade;

import java.util.function.BiFunction;

/**
 * Create trade with snapshot and adjust {@link Order#cumQty} of matched orders.
 *
 * @author ratzlow@gmail.com
 * @since 2015-07-23
 */
public class TradeFactory {

    public static Trade createTrade(Order order, Order otherSideOrder, double price,
                                BiFunction<Order, Order, Integer> takeQtyStrategy ) {

        Side otherSide = otherSideOrder.getSide();
        Order buy = otherSide == Side.BUY ? otherSideOrder.mutableClone() : order.mutableClone();
        Order sell = otherSide == Side.SELL ? otherSideOrder.mutableClone() : order.mutableClone();

        int takeQty = takeQtyStrategy.apply(buy, sell);
        if ( takeQty < 1 ) {
            throw new MatchException("Cannot create a trade if nothing matched!");
        }

        buy.setCumQty( buy.getCumQty() + takeQty );
        sell.setCumQty( sell.getCumQty() + takeQty );

        return Trade.of().setBuy(buy).setSell(sell).setExecutionQty(takeQty).setPrice(price).setExecType(ExecType.TRADE);
    }

    public static boolean isCrossedPrice( double bid, double ask ) { return bid >= ask; }
}
