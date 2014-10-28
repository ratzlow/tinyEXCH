package net.tinyexch.ob.match;

import net.tinyexch.exchange.trading.form.auction.DefaultPriceDeterminationPhase;
import net.tinyexch.exchange.trading.form.auction.PriceDeterminationPhase;
import net.tinyexch.exchange.trading.form.auction.PriceDeterminationResult;
import net.tinyexch.ob.Orderbook;
import net.tinyexch.order.Order;
import net.tinyexch.order.OrderType;
import net.tinyexch.order.Side;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static net.tinyexch.ob.TestConstants.ROUNDING_DELTA;
import static net.tinyexch.ob.match.Algos.searchClosestAsk;
import static net.tinyexch.ob.match.Algos.searchClosestBid;

/**
 * Test various matching strategies depending on a given orderbook situation.
 *
 * @author ratzlow@gmail.com
 * @since 2014-10-12
 */
public class AuctionMatchTest {

    private final Orderbook book_Ex_1 = new Orderbook( new Order[]{ buy(1, 202, 200), buy(2, 201, 200), buy(3, 200, 300) },
                                               new Order[]{ sell(4, 200, 100), sell(5, 198, 200), sell(6, 197, 400) } );

    private final Orderbook book_Ex_2 = new Orderbook( new Order[]{ buy(1, 202, 400), buy(2, 201, 200) },
                                               new Order[]{ sell(1, 199, 300), sell(2, 198, 200) } );

    private final Orderbook book_Ex_3 = new Orderbook( new Order[]{ buy(1, 202, 300), buy(2, 201, 200) },
                                               new Order[]{ sell(3, 199, 400), sell(4, 198, 200) } );

    /**
     * There is exactly one limit at which the highest order volume can be executed and which has the lowest surplus.
     * Corresponding to this limit, the auction price is fixed at € 200.
     */
    @Test
    public void testAuctionPriceEqLimit_Ex1() {
        PriceDeterminationResult result = determinePrice(book_Ex_1);
        Assert.assertEquals(200D, result.getBidPrice(), ROUNDING_DELTA);
        Assert.assertEquals(200D, result.getAskPrice(), ROUNDING_DELTA);
        Assert.assertEquals( 0, result.getAskSurplus() );
        Assert.assertEquals( 0, result.getBidSurplus() );
        Assert.assertEquals( 200D, result.getAuctionPrice(), ROUNDING_DELTA );
    }


    /**
     * There are several possible limits and there is a surplus on the bid.
     * Corresponding to the highest limit, the auction price is fixed at € 201.
     */
    @Test
    public void testAuctionPriceEqHighestLimit_Ex2() {
        PriceDeterminationResult result = determinePrice(book_Ex_2);
        Assert.assertEquals( 201D, result.getBidPrice(), ROUNDING_DELTA);
        Assert.assertEquals( 199D, result.getAskPrice(), ROUNDING_DELTA);
        Assert.assertEquals( 0, result.getAskSurplus() );
        Assert.assertEquals( 100, result.getBidSurplus() );
        Assert.assertEquals( 201D, result.getAuctionPrice(), ROUNDING_DELTA );
    }


    /**
     * There are several possible limits and there is a surplus on the ask.
     * Corresponding to the lowest limit, the auction price is fixed at € 199.
     */
    @Test
    public void testAuctionPriceEqLowestLimit_Ex3() {
        PriceDeterminationResult result = determinePrice(book_Ex_3);
        Assert.assertEquals( 201D, result.getBidPrice(), ROUNDING_DELTA);
        Assert.assertEquals( 199D, result.getAskPrice(), ROUNDING_DELTA);
        Assert.assertEquals( 100, result.getAskSurplus() );
        Assert.assertEquals( 0, result.getBidSurplus() );
        Assert.assertEquals( 199D, result.getAuctionPrice(), ROUNDING_DELTA );
    }


    // TODO (FRa) : (FRa) : move to AlgosTest
    @Test
    public void testOrderbookBuilding() {
        // sorted with best coming first?!
        List<Order> orderedBuys = book_Ex_1.getBuySide().getBest(DefaultPriceDeterminationPhase.BUY_PRICE_ORDERING);
        List<Order> orderedSells = book_Ex_1.getSellSide().getBest(DefaultPriceDeterminationPhase.SELL_PRICE_ORDERING);

        Assert.assertArrayEquals(new Integer[]{1, 2, 3},
                orderedBuys.stream().map(order -> Integer.parseInt(order.getClientOrderID()))
                        .collect(Collectors.toList()).toArray());
        Assert.assertArrayEquals(new Integer[]{6, 5, 4},
                orderedSells.stream().map(order -> Integer.parseInt(order.getClientOrderID()))
                        .collect(Collectors.toList()).toArray());

        double[] bidPrices = orderedBuys.stream().mapToDouble(Order::getPrice).toArray();
        double[] askPrices = orderedSells.stream().mapToDouble(Order::getPrice).toArray();

        double worstMatchableAskPrice = searchClosestAsk(orderedBuys.get(0).getPrice(), askPrices);
        double worstMatchableBidPrice = searchClosestBid(orderedSells.get(0).getPrice(), bidPrices);

        Assert.assertEquals(200, worstMatchableBidPrice, ROUNDING_DELTA);
        Assert.assertEquals(200, worstMatchableAskPrice, ROUNDING_DELTA);
    }


    private PriceDeterminationResult determinePrice( Orderbook orderbook ) {
        PriceDeterminationPhase phase = new DefaultPriceDeterminationPhase(orderbook);
        return phase.determinePrice();
    }

    private Order buy(int clientOrderID, double price, int qty) {
        return newOrder( Side.BUY, clientOrderID, price, qty );
    }

    private Order sell(int clientOrderID, double price, int qty) {
        return newOrder( Side.SELL, clientOrderID, price, qty );
    }

    private Order newOrder(Side side, int clientOrderID, double price, int qty) {
        return Order.of( Integer.toString(clientOrderID), side ).setPrice(price)
                .setOrderQty(qty).setOrderType(OrderType.LIMIT);
    }
}
