package net.tinyexch.exchange.trading.form;

import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.auction.AuctionState;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static net.tinyexch.exchange.trading.form.auction.AuctionState.*;
import static net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState.RUNNING;
import static net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState.STOPPED;
import static org.junit.Assert.assertEquals;

/**
 * Test the cycle through the states.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-04
 */
public class ChangeTradingFormStateTest {

    @Test
    public void testContinuousTradingStateCycle() {
        ContinuousTrading trading = new ContinuousTrading();
        // before anything happened
        assertEquals(STOPPED, trading.getDefaultState());
        assertEquals(trading.getCurrentState(), trading.getDefaultState());

        trading.transitionTo(RUNNING);
        assertEquals(RUNNING, trading.getCurrentState());

        trading.transitionTo( STOPPED);
        assertEquals(STOPPED, trading.getCurrentState());

        trading.transitionTo( STOPPED );
        assertEquals(STOPPED, trading.getCurrentState());
    }


    @Test
    public void testAuctionOK() {

        List<AuctionState> states = new ArrayList<>();

        Auction auction = new Auction(
            state -> { states.add(state); },    // state listener
            order -> {},                        // call phase
            () -> {},                           // price determination
            () -> {}                            // OB balancing
        );

        // before anything happened
        assertEquals(INACTIVE, auction.getDefaultState());
        assertEquals(auction.getCurrentState(), auction.getDefaultState());

        auction.startCallPhase();
        assertEquals( CALL_RUNNING, auction.getCurrentState());
        auction.stopCallPhase();
        assertEquals( INACTIVE, auction.getCurrentState());

        states.clear();
        auction.determinePrice();
        assertEquals(PRICE_DETERMINATION_RUNNING, states.get(0));
        assertEquals(INACTIVE, states.get(1));
        assertEquals(INACTIVE, auction.getCurrentState());

        states.clear();
        auction.balanceOrderbook();
        assertEquals(ORDERBOOK_BALANCING_RUNNING, states.get(0));
        assertEquals(INACTIVE, states.get(1) );
        assertEquals(INACTIVE, auction.getCurrentState());
    }


    @Test(expected = IllegalStateException.class)
    public void testAuctionNotOKJumpAhead() {
        Auction auction = new Auction();
        auction.transitionTo(CALL_RUNNING);
        auction.transitionTo(PRICE_DETERMINATION_RUNNING);
    }


    @Test(expected = IllegalStateException.class)
    public void testAuctionNotOKJumpBack() {
        Auction auction = new Auction();
        auction.transitionTo(CALL_RUNNING);
        auction.transitionTo(PRICE_DETERMINATION_RUNNING);
        auction.transitionTo(CALL_RUNNING);
    }
}
