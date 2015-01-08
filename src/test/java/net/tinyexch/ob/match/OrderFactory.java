package net.tinyexch.ob.match;

import net.tinyexch.order.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * Create simple orders for test purpose.
 *
 * @author ratzlow@gmail.com
 * @since 2014-10-29
 */
public class OrderFactory {
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm:ss";
    private static final String TODAY = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN) );
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN + " " + TIME_PATTERN); //


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
     * @param timestamp #TIME_PATTERN
     */
    public static Order buyL(double price, int qty, Instant timestamp ) {
        return newOrder( Side.BUY, price, qty, OrderType.LIMIT ).setTimestamp(timestamp);
    }

    public static Order sellL(double price, int qty) {
        return newOrder( Side.SELL, price, qty, OrderType.LIMIT );
    }

    public static Order sellL(double price, int qty, Instant timestamp) {
        return newOrder( Side.SELL, price, qty, OrderType.LIMIT ).setTimestamp(timestamp);
    }

    public static Order buyM(int qty) {
        return newOrder( Side.BUY, 0, qty, OrderType.MARKET );
    }

    public static Order buyMtoL(int qty) {
        return newOrder( Side.BUY, 0, qty, OrderType.MARKET_TO_LIMIT );
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

    public static Order sellMtoL(int qty) {
        return newOrder( Side.SELL, 0, qty, OrderType.MARKET_TO_LIMIT );
    }

    public static Order sellM(int qty, Instant timestamp ) {
        return newOrder( Side.SELL, 0, qty, OrderType.MARKET ).setTimestamp(timestamp);
    }

    public static Order newOrder(Side side, double price, int qty, OrderType type ) {
        return Order.of( Integer.toString(++clientOrderIdSequence), side ).setPrice(price)
                .setOrderQty(qty).setOrderType(type);
    }

    /**
     * @param time formatted according to {@link #TIME_PATTERN}
     * @return today with given #time
     */
    public static Instant time( String time ) {
        TemporalAccessor temporalAccessor = DATE_TIME_FORMATTER.parse(TODAY + " " + time);
        return LocalDateTime.from(temporalAccessor).toInstant(ZoneOffset.UTC);
    }
}
