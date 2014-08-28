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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static net.tinyexch.exchange.trading.form.auction.AuctionState.*;
import static net.tinyexch.exchange.trading.form.auction.AuctionStateChange.*;

/**
 * Check that we can specify the timing and order of transition of trading model phases and between trading forms,
 * e.g. Auction -> ContinuousTrading -> Auction
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-10
 */
// // TODO (FRa) : (FRa) : add blocking call, PD, OBB strategies and check if life cycle is preserved; call must reject orders after end
public class TradingModelScheduledStateChangeTest {
    private final Logger LOGGER = LoggerFactory.getLogger(TradingModelScheduledStateChangeTest.class);


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

        tradingModelPhaseChanger.startTrading( oneSingleAuction() );
        AuctionState[] expectedFlippedStates = {CALL_RUNNING, CALL_STOPPED,
                PRICE_DETERMINATION_RUNNING, PRICE_DETERMINATION_STOPPED,
                ORDERBOOK_BALANCING_RUNNING, ORDERBOOK_BALANCING_STOPPED};
        latch.await(50, TimeUnit.MILLISECONDS);
        Assert.assertArrayEquals("Recorded state changes: " + actualFlippedStates,
                expectedFlippedStates, actualFlippedStates.toArray());
    }


    @Test
    public void testScheduledPhasesForTomorrow_NoTradingExpected() throws InterruptedException {
        Auction auction = new Auction();
        AtomicInteger stateChanges = new AtomicInteger(0);
        auction.register( state -> stateChanges.incrementAndGet() );

        AuctionTradingModel tradingModel = new AuctionTradingModel( new TradingModelProfile(), auction );
        TradingModelPhaseChanger phaseChanger = new TradingModelPhaseChanger( tradingModel );
        LocalDate yesterday = LocalDate.now().minusDays(1);
        phaseChanger.startTrading( new TradingCalendar(yesterday) );
        Thread.sleep(20);
        Assert.assertEquals("No trading scheduled for today!", 0, stateChanges.get());
    }


    @Test
    public void testRandomTimeTriggerWithinGivenOffsetRange() {
        LocalTime now = LocalTime.now();
        long nowMillis = now.get(ChronoField.MILLI_OF_DAY);
        int maxDuration = 100;

        for ( int min=0; min < maxDuration; min++ ) {
            TradingCalendar cal = new TradingCalendar();

            Assert.assertEquals(0, cal.getTriggers().size());
            cal.addVariantDurationTrigger(AuctionStateChange.START_CALL, now, min, maxDuration, ChronoUnit.MILLIS);
            Assert.assertEquals(1, cal.getTriggers().size());

            TradingPhaseTrigger trigger = cal.getTriggers().get(0);
            LocalTime triggerFixedTime = trigger.getFixedTime();
            int triggerStampMillis = triggerFixedTime.get(ChronoField.MILLI_OF_DAY);
            LOGGER.debug("now={} range=[{} -{}] triggerTime={}", nowMillis, min, maxDuration, triggerStampMillis);
            Assert.assertTrue(
                    String.format("triggerStampMillis=%d nowMillis+min=%d", triggerStampMillis, nowMillis + min),
                    triggerStampMillis >= nowMillis + min);
            Assert.assertTrue(
                    String.format("triggerStampMillis=%d nowMillis+max=%d", triggerStampMillis, nowMillis + maxDuration),
                    triggerStampMillis <= nowMillis + maxDuration);
        }
    }


    /*
        start at 3:30 with CALL_START | and then
        send after 45-50min CALL_STOP | and then
        send immediately    PD_START  | and then
        wait for            INACTIVE  | and then
        send immediately OB_Bal_START | and then
        wait for         INACTIVE
    */
    private TradingCalendar oneSingleAuction() {
        LocalTime now = LocalTime.now();
        ChronoUnit unit = ChronoUnit.MILLIS;
        LocalTime startTradingTime = now.plus(futureAuctionStartOffsetInMillis, unit);

        TradingCalendar tradingCalendar = new TradingCalendar( LocalDate.now() );
        tradingCalendar.addFixedTimeTrigger(START_CALL, startTradingTime)
                .addVariantDurationTrigger(STOP_CALL, startTradingTime, 3, 10, unit)
                .addWaitTrigger(START_PRICEDETERMINATION, CALL_STOPPED)
                .addWaitTrigger(START_ORDERBOOK_BALANCING, PRICE_DETERMINATION_STOPPED);

        return tradingCalendar;
    }

    private TradingCalendar continuousTradingInConnectionWithAuctions_IntradayClosingAuctionNotScheduled() {
        // opening auction
        // continuous trading (interrupted by auction)
        // closing auction
        return null;
    }

    private TradingCalendar continuousTradingInConnectionWithAuctions_IntradayClosingAuctionScheduled() {
        // opening auction
        // continuous trading (interrupted by auction)
        // intraday closing auction
        // continuous trading (interrupted by auction)
        // EOD auction
        return null;
    }
}
