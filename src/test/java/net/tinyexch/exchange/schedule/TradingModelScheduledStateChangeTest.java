package net.tinyexch.exchange.schedule;

import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.auction.AuctionState;
import net.tinyexch.exchange.trading.form.auction.AuctionStateChange;
import net.tinyexch.exchange.trading.model.AuctionTradingModel;
import net.tinyexch.exchange.trading.model.TradingModelProfile;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static net.tinyexch.exchange.trading.form.auction.AuctionState.*;
import static net.tinyexch.exchange.trading.form.auction.AuctionStateChange.*;

/**
 * Check that we can specify the timing and order of transition of trading model phases and between trading forms,
 * e.g. Auction -> ContinuousTrading -> Auction
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-10
 */
// TODO (FRa) : (FRa) : test randomness of call end
// supply a TradingCal for the scheduler
// add blocking call, PD, OBB strategies and check if life cycle is preserved; call must reject orders after end
public class TradingModelScheduledStateChangeTest {
    private final Logger LOGGER = LoggerFactory.getLogger(TradingModelScheduledStateChangeTest.class);
    private final Random random = new Random(LocalTime.now().getNano());

    /** Defines how many ms after test data is wired the actual auction will be run */
    private final long futureAuctionStartOffsetInMillis = 10;

    @Test
    public void testAuctionImmediatePhaseSwitch() {
        // TODO: add state listener to check if transitions are in order
        TradingModelProfile profile = new TradingModelProfile();
        Auction auction = new Auction();
        AuctionTradingModel auctionTradingModel = new AuctionTradingModel(profile, auction);
        Arrays.stream( AuctionStateChange.values() ).forEach(auctionTradingModel::moveTo);
    }


    @Test
    public void testScheduledPhaseSwitch() throws InterruptedException {
        Auction auction = new Auction();
        CountDownLatch latch = new CountDownLatch(AuctionState.values().length);
        List<AuctionState> actualFlippedStates = new ArrayList<>();
        auction.register( newState -> {
            LOGGER.debug("Test listener was fired with {}", newState);
            actualFlippedStates.add(newState);
            latch.countDown();
        });

        AuctionTradingModel auctionModel = new AuctionTradingModel(new TradingModelProfile(), auction);
        TradingModelPhaseChanger tradingModelPhaseChanger = new TradingModelPhaseChanger( auctionModel );

        tradingModelPhaseChanger.startTrading( buildTradingDay() );
        AuctionState[] expectedFlippedStates = {CALL_RUNNING, CALL_STOPPED,
                PRICE_DETERMINATION_RUNNING, PRICE_DETERMINATION_STOPPED,
                ORDERBOOK_BALANCING_RUNNING, ORDERBOOK_BALANCING_STOPPED};
        latch.await(50, TimeUnit.MILLISECONDS);
        Assert.assertArrayEquals("Recorded state changes: " + actualFlippedStates,
                expectedFlippedStates, actualFlippedStates.toArray());
    }


    //-------------------------------------------------------------------------------------------------
    // helpers to setup the schedule for trading
    //-------------------------------------------------------------------------------------------------

    /*
        start at 3:30 with CALL_START | and then
        send after 45-50min CALL_STOP | and then
        send immediately    PD_START  | and then
        wait for            INACTIVE  | and then
        send immediately OB_Bal_START | and then
        wait for         INACTIVE
    */
    private List<TradingPhaseTrigger> buildTradingDay() {
        LocalTime now = LocalTime.now();
        ChronoUnit unit = ChronoUnit.MILLIS;

        List<TradingPhaseTrigger> auctionPhases = new ArrayList<>();
        TradingPhaseTrigger startCallPhase =
                createFixedTimeTrigger( START_CALL, now.plus(futureAuctionStartOffsetInMillis, unit));
        TradingPhaseTrigger endCallPhase =
                createVariantDurationTrigger( STOP_CALL, startCallPhase.getFixedTime(), 3, 10, unit);
        TradingPhaseTrigger startPriceDetermination =
                createWaitTrigger( START_PRICEDETERMINATION, CALL_STOPPED);
        TradingPhaseTrigger startOrderbookBalancing =
                createWaitTrigger( START_ORDERBOOK_BALANCING, PRICE_DETERMINATION_STOPPED);

        auctionPhases.add(startCallPhase);
        auctionPhases.add(endCallPhase);
        auctionPhases.add(startPriceDetermination);
        auctionPhases.add(startOrderbookBalancing);

        return auctionPhases;
    }

    private TradingPhaseTrigger createWaitTrigger( AuctionStateChange stateChange, AuctionState waitFor ) {
        return new TradingPhaseTrigger( stateChange, waitFor );
    }

    private TradingPhaseTrigger createFixedTimeTrigger(AuctionStateChange stateChange, LocalTime time) {
        return new TradingPhaseTrigger( stateChange, time );
    }

    private TradingPhaseTrigger createVariantDurationTrigger(AuctionStateChange stateChange, LocalTime from,
                                                             int plusMinDuration, int plusMaxDuration, ChronoUnit unit ) {
        int duration = random.nextInt(plusMaxDuration - plusMinDuration) + plusMaxDuration;
        LocalTime time = from.plus(duration, unit);
        return new TradingPhaseTrigger( stateChange, time );
    }
}
