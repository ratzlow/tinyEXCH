package net.tinyexch.ob.match;

import net.tinyexch.order.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Create simple orders for test purpose.
 *
 * @author ratzlow@gmail.com
 * @since 2014-10-29
 */
public class OrderFactory {
    private static int clientOrderIdSequence = 0;

    public static Order buyL(double price, int qty) {
        return newOrder( Side.BUY, price, qty, OrderType.LIMIT );
    }

    public static Order buySMO(double stopPrice, int qty) {
        return newOrder( Side.BUY, 0, qty, OrderType.STRIKE_MATCH ).setStopPrice(stopPrice)
                .setDiscretionLimitType(DiscretionLimitType.OR_WORSE)
                .setTradingSessionSubID(TradingSessionSubID.ClosingOrClosingAuction);
    }

    /**
     * @param timestamp #timePattern
     */
    public static Order buyL(double price, int qty, Instant timestamp ) {
        return newOrder( Side.BUY, price, qty, OrderType.LIMIT ).setTimestamp(timestamp);
    }

    public static Order sellL(double price, int qty) {
        return newOrder( Side.SELL, price, qty, OrderType.LIMIT );
    }

    public static Order buyM(int qty) {
        return newOrder( Side.BUY, 0, qty, OrderType.MARKET );
    }

    public static Order buyM(int qty, Instant timestamp ) {
        return newOrder( Side.BUY, 0, qty, OrderType.MARKET ).setTimestamp(timestamp);
    }

    public static Order buyH(double price, int qty) {
        return newOrder(Side.BUY, price, qty, OrderType.HIDDEN);
    }

    public static Order sellM(int qty) {
        return newOrder( Side.SELL, 0, qty, OrderType.MARKET );
    }

    public static Order sellM(int qty, Instant timestamp ) {
        return newOrder( Side.SELL, 0, qty, OrderType.MARKET ).setTimestamp(timestamp);
    }

    public static Order newOrder(Side side, double price, int qty, OrderType type ) {
        return Order.of( Integer.toString(++clientOrderIdSequence), side ).setPrice(price)
                .setOrderQty(qty).setOrderType(type);
    }

    public static Instant time(int hour, int min, int sec) {
        return LocalDateTime.now().withHour(hour).withMinute(min).withSecond(sec).toInstant(ZoneOffset.UTC);
    }
}
