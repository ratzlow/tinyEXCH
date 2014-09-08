package net.tinyexch.exchange.schedule;

import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.auction.AuctionState;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState;
import net.tinyexch.exchange.trading.model.AuctionTradingModel;
import net.tinyexch.exchange.trading.model.ContinuousTradingInterruptedByAuctions;
import net.tinyexch.exchange.trading.model.TradingFormRunType;
import net.tinyexch.exchange.trading.model.TradingModelProfile;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.tinyexch.exchange.trading.form.auction.AuctionState.*;
import static net.tinyexch.exchange.schedule.StateChangerFactory.createContinuousTrading_ContTrading;
import static net.tinyexch.exchange.trading.model.TradingFormRunType.*;

/**
 * Check that we can specify the timing and order of transition of trading model phases and between trading forms,
 * e.g. Auction -> ContinuousTrading -> Auction
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-10
 */
// TODO (FRa) : (FRa) : add blocking call, PD, OBB strategies and check if life cycle is preserved; call must reject orders after end
// TODO (FRa) : (FRa) : add test for continuousTradingInConnectionWithAuctions_IntradayClosingAuctionScheduled
public class TradingModelScheduledStateChangeTest {
    private final Logger LOGGER = LoggerFactory.getLogger(TradingModelScheduledStateChangeTest.class);

    private static final AuctionState[] EXPECTED_FLIPPED_AUCTION_STATES = {
            CALL_RUNNING, CALL_STOPPED,
            PRICE_DETERMINATION_RUNNING, PRICE_DETERMINATION_STOPPED,
            ORDERBOOK_BALANCING_RUNNING, ORDERBOOK_BALANCING_STOPPED};

    private static final ContinuousTradingState[] EXPECTED_CONTINUOUS_TRADING_STATES = {
            ContinuousTradingState.RUNNING, ContinuousTradingState.STOPPED};

    /** Defines how many ms after test data is wired the actual auction will be run */
    private final long futureAuctionStartOffsetInMillis = 25;
    private final ChronoUnit unit = ChronoUnit.MILLIS;

    @Test
    public void testScheduledPhaseWithSingleAuction() throws InterruptedException {
        assertPureAuctionTrading( Arrays.asList(EXPECTED_FLIPPED_AUCTION_STATES),
                new TradingFormRunType[]{OPENING_AUCTION}, multipleAuction(OPENING_AUCTION) );
    }

    @Test
    public void testScheduledPhaseWithThreeAuctions() throws InterruptedException {
        List<Enum> expectedStates = new ArrayList<>();
        expectedStates.addAll(Arrays.asList(EXPECTED_FLIPPED_AUCTION_STATES));
        expectedStates.addAll(Arrays.asList(EXPECTED_FLIPPED_AUCTION_STATES));
        expectedStates.addAll(Arrays.asList(EXPECTED_FLIPPED_AUCTION_STATES));
        TradingFormRunType[] tradingFormRunTypes = {OPENING_AUCTION, INTRADAY_AUCTION, CLOSING_AUCTION};
        assertPureAuctionTrading( expectedStates, tradingFormRunTypes, multipleAuction(tradingFormRunTypes));
    }

    // TODO (FRa) : (FRa) : enable time range schedule (random offset) after fixed Time & on response of waitFor state
    // TODO (FRa) : (FRa) : enable kick off interrupting auctions -> check docs for nature of interrupting auction
    @Test
    public void testScheduledPhaseSwitchContinuousTradingWithOpenAndCloseAuction() throws InterruptedException {

        ContinuousTradingInterruptedByAuctions contTradingWithOpeningAndClosingAuction =
                new ContinuousTradingInterruptedByAuctions( new TradingModelProfile());

        CountDownLatch latch = new CountDownLatch( 3 * EXPECTED_FLIPPED_AUCTION_STATES.length );
        TradingProcess tradingProcess = new TradingProcess( state -> latch.countDown() );
        tradingProcess.startTrading(continuousTradingWithOpeningAndClosingAuction(), contTradingWithOpeningAndClosingAuction);
        latch.await(1, TimeUnit.SECONDS);

        List<Enum> actualFlippedStates = tradingProcess.getFiredStateChanges().stream().map(
                TradingProcess.FiredStateChange::getNewState).collect(Collectors.toList());
        List<Enum> allExpectedStates = new ArrayList<>();
        allExpectedStates.addAll( Arrays.asList(EXPECTED_FLIPPED_AUCTION_STATES));
        allExpectedStates.addAll( Arrays.asList(EXPECTED_CONTINUOUS_TRADING_STATES));
        allExpectedStates.addAll( Arrays.asList(EXPECTED_FLIPPED_AUCTION_STATES));
        Assert.assertArrayEquals("Expected: " + allExpectedStates + " actual: " + actualFlippedStates,
                allExpectedStates.toArray(),
                actualFlippedStates.toArray());
    }


    @Test
    public void testScheduledPhasesForTomorrow_NoTradingExpected() throws InterruptedException {
        AuctionTradingModel tradingModel = new AuctionTradingModel( new TradingModelProfile());
        TradingProcess tradingProcess = new TradingProcess();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        tradingProcess.startTrading(new TradingCalendar(yesterday), tradingModel);
        Thread.sleep(20);
        Assert.assertNull(tradingModel.getTradingFormRunType());
        Assert.assertEquals("No trading scheduled for today!", 0, tradingProcess.getFiredStateChanges().size());
    }


    @Test
    public void testRandomTimeTriggerWithinGivenOffsetRange() {
        LocalTime now = LocalTime.now();
        long nowMillis = now.get(ChronoField.MILLI_OF_DAY);
        int maxDuration = 100;

        for ( int min=0; min < maxDuration; min++ ) {
            TradingCalendar cal = new TradingCalendar();

            Assert.assertEquals(0, cal.getTriggers().size());
            cal.addVariantDurationTrigger(StateChangerFactory.startCall(), now, min, maxDuration, unit);
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

    private void assertPureAuctionTrading( List<Enum> expectedStateChanges,
                                           TradingFormRunType[] expectedTradingFormRuntimes,
                                           TradingCalendar tradingCalendar ) throws InterruptedException {

        AuctionTradingModel auctionModel = new AuctionTradingModel(new TradingModelProfile());
        CountDownLatch latch = new CountDownLatch(expectedStateChanges.size());
        TradingProcess tradingProcess = new TradingProcess( state -> latch.countDown() );
        tradingProcess.startTrading(tradingCalendar, auctionModel);
        latch.await(expectedStateChanges.size() * 100, TimeUnit.MILLISECONDS);

        List<Enum> actualStateChanges = tradingProcess.getFiredStateChanges().stream().map(
                TradingProcess.FiredStateChange::getNewState).collect(Collectors.toList());
        Assert.assertArrayEquals("Recorded state changes: " + actualStateChanges,
                expectedStateChanges.toArray(),
                actualStateChanges.toArray());
        Assert.assertArrayEquals( expectedTradingFormRuntimes, tradingProcess.getFiredTradingFormRunTypes().toArray() );
    }


    private TradingCalendar multipleAuction(TradingFormRunType... tradingFormRunTypes) {
        TradingCalendar tradingCalendar = new TradingCalendar(LocalDate.now());
        long offset = 0;
        for ( TradingFormRunType runType : tradingFormRunTypes) {
            offset += futureAuctionStartOffsetInMillis;
            addAuctionPhases(tradingCalendar, runType, offset);
        }
        return tradingCalendar;
    }

    private TradingCalendar addAuctionPhases(TradingCalendar tradingCalendar, TradingFormRunType runType, long millisOffset) {
        LocalTime now = LocalTime.now();
        LocalTime startTradingTime = now.plus(futureAuctionStartOffsetInMillis, unit).plus(millisOffset, unit);
        tradingCalendar.addAuction(AuctionSchedule.kickOff(StateChangerFactory.createAuction_Auction(Auction::new, runType))
                .startingCallPhaseAt(startTradingTime)
                .andLastForAtLeast(Duration.ofMillis(3))
                .withRandomBufferBetween(Duration.ofMillis(10)));
        return tradingCalendar;
    }


    private TradingCalendar continuousTradingWithOpeningAndClosingAuction() {
        LocalTime now = LocalTime.now();
        LocalTime startTradingTime = now.plus(futureAuctionStartOffsetInMillis, unit);
        LocalDate today = LocalDate.now();

        // opening auction
        TradingCalendar tradingCalendar = new TradingCalendar(today);
        TradingFormInitializer<ContinuousTradingInterruptedByAuctions> openingAuction =
                StateChangerFactory.createAuction_ContTrading(Auction::new, TradingFormRunType.OPENING_AUCTION);
        AuctionSchedule openingSchedule = AuctionSchedule.kickOff(openingAuction)
                .startingCallPhaseAt(startTradingTime)
                .andLastForAtLeast(Duration.ofMillis(3))
                .withRandomBufferBetween(Duration.ofMillis(10));
        tradingCalendar.addAuction(openingSchedule);

        // continuous trading (interrupted by auction)
        LocalTime startContinuousTradingTime = startTradingTime.plus(50, unit);
        LocalTime stopContinuousTradingTime = startContinuousTradingTime.plus(50, unit);
        tradingCalendar.addContinuousTrading( new ContinuousTradingSchedule(
                createContinuousTrading_ContTrading(ContinuousTrading::new, TradingFormRunType.CONTINUOUS_TRADING),
                startContinuousTradingTime, stopContinuousTradingTime));

        // closing auction
        LocalTime startClosingAuctionTime = stopContinuousTradingTime.plus(200, unit);
        TradingFormInitializer<ContinuousTradingInterruptedByAuctions> closingAuction =
                StateChangerFactory.createAuction_ContTrading(Auction::new, TradingFormRunType.CLOSING_AUCTION);
        AuctionSchedule closeSchedule = AuctionSchedule.kickOff(closingAuction)
                .startingCallPhaseAt(startClosingAuctionTime)
                .andLastForAtLeast(Duration.ofMillis(3))
                .withRandomBufferBetween(Duration.ofMillis(10));
        tradingCalendar.addAuction( closeSchedule );

        return tradingCalendar;
    }
}
