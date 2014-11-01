package net.tinyexch.ob.match;

import net.tinyexch.exchange.trading.form.auction.DefaultPriceDeterminationPhase;
import net.tinyexch.exchange.trading.form.auction.PriceDeterminationPhase;
import net.tinyexch.exchange.trading.form.auction.PriceDeterminationResult;
import net.tinyexch.ob.Orderbook;
import net.tinyexch.order.Order;
import org.junit.Assert;
import org.junit.Test;

import static net.tinyexch.ob.TestConstants.ROUNDING_DELTA;
import static net.tinyexch.ob.match.OrderFactory.*;

/**
 * Test various matching strategies depending on a given orderbook situation.
 *
 * @author ratzlow@gmail.com
 * @since 2014-10-12
 */
public class AuctionMatchTest {

    private final Orderbook book_Ex_1 = new Orderbook( new Order[]{ buyL(202, 200), buyL(201, 200), buyL(200, 300) },
                                                       new Order[]{ sellL(200, 100), sellL(198, 200), sellL(197, 400) } );

    private final Orderbook book_Ex_2 = new Orderbook( new Order[]{ buyL(202, 400), buyL(201, 200) },
                                                       new Order[]{ sellL(199, 300), sellL(198, 200) } );

    private final Orderbook book_Ex_3 = new Orderbook( new Order[]{ buyL(202, 300), buyL(201, 200) },
                                                       new Order[]{ sellL(199, 400), sellL(198, 200) } );

    private final Orderbook book_Ex_4 = new Orderbook( new Order[]{ buyM(100), buyL(199, 100) },
                                                       new Order[]{ sellM(100), sellL(202, 100) } );


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


    /**
     * There are several possible limits and there is both an ask surplus and a bid surplus.
     *
     * The auction price either equals the reference price or is fixed according to the limit nearest to the reference
     * price.
     */
    // TODO (FRa) : (FRa) : add test where ref price is exactly in the middle of 2 possible limits, check rules
    @Test
    public void testAuctionPriceEqualsReferencePrice_Ex4_1() {

        PriceDeterminationResult result_1 = determinePrice(book_Ex_4, 199D);
        Assert.assertEquals( "If the reference price is € 199, the auction price will be € 199.",
                199D, result_1.getAuctionPrice(), ROUNDING_DELTA );

        PriceDeterminationResult result_2 = determinePrice(book_Ex_4, 200D);
        Assert.assertEquals( "If the reference price is € 200, the auction price will be € 199.",
                199D, result_2.getAuctionPrice(), ROUNDING_DELTA );
    }


    private PriceDeterminationResult determinePrice( Orderbook orderbook ) {
        PriceDeterminationPhase phase = new DefaultPriceDeterminationPhase(orderbook);
        return phase.determinePrice();
    }

    private PriceDeterminationResult determinePrice( Orderbook orderbook, double referencePrice  ) {
        PriceDeterminationPhase phase = new DefaultPriceDeterminationPhase(orderbook, referencePrice );
        return phase.determinePrice();
    }
}
