package net.tinyexch.exchange.trading.form;

import net.tinyexch.exchange.event.MarketRunner;
import net.tinyexch.exchange.event.NotificationListener;
import net.tinyexch.exchange.event.produce.StateChangedEvent;
import net.tinyexch.exchange.schedule.TradingCalendar;
import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.auction.AuctionState;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeTradingFormStateTest.class);

    @Test
    public void testContinuousTradingStateCycle() {
        ContinuousTrading trading = new ContinuousTrading(NotificationListener.NO_OP);
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
        // TODO (FRa) : (FRa) : use mock lib for this interface mockout
        NotificationListener testListener = new NotificationListener() {
            @Override public void init(MarketRunner marketRunner, TradingCalendar tradingCalendar) { }

            @Override
            public <T> void fire(T notification) {
                if (notification instanceof StateChangedEvent) {
                    StateChangedEvent e = (StateChangedEvent) notification;
                    states.add((AuctionState) e.getCurrent());

                } else {
                    LOGGER.info("Ignore sent event {}" + notification);
                }
            }
        };

        Auction auction = new Auction(testListener);

        // before anything happened
        assertEquals(INACTIVE, auction.getDefaultState());
        assertEquals(auction.getCurrentState(), auction.getDefaultState());

        auction.startCallPhase();
        assertEquals( CALL_RUNNING, auction.getCurrentState());
        auction.stopCallPhase();
        assertEquals( CALL_STOPPED, auction.getCurrentState());

        states.clear();
        auction.determinePrice();
        assertEquals(PRICE_DETERMINATION_RUNNING, states.get(0));
        assertEquals(PRICE_DETERMINATION_STOPPED, states.get(1));
        assertEquals(states.get(1), auction.getCurrentState());

        states.clear();
        auction.balanceOrderbook();
        assertEquals(ORDERBOOK_BALANCING_RUNNING, states.get(0));
        assertEquals(ORDERBOOK_BALANCING_STOPPED, states.get(1) );
        assertEquals(states.get(1), auction.getCurrentState());
    }


    @Test(expected = IllegalStateException.class)
    public void testAuctionNotOKJumpAhead() {
        Auction auction = new Auction(NotificationListener.NO_OP);
        auction.transitionTo(CALL_RUNNING);
        auction.transitionTo(PRICE_DETERMINATION_RUNNING);
    }


    @Test(expected = IllegalStateException.class)
    public void testAuctionNotOKJumpBack() {
        Auction auction = new Auction(NotificationListener.NO_OP);
        auction.transitionTo(CALL_RUNNING);
        auction.transitionTo(PRICE_DETERMINATION_RUNNING);
        auction.transitionTo(CALL_RUNNING);
    }
}
