package net.tinyexch.exchange.event;

import net.tinyexch.exchange.event.produce.StateChangedEvent;
import net.tinyexch.exchange.event.produce.StateChangedEventHandler;
import net.tinyexch.exchange.schedule.AuctionSchedule;
import net.tinyexch.exchange.schedule.ContinuousTradingSchedule;
import net.tinyexch.exchange.schedule.TradingCalendar;
import net.tinyexch.exchange.schedule.TradingPhaseTrigger;
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
import static net.tinyexch.exchange.trading.model.TradingFormRunType.*;

/**
 * Control the switching in the life cycle vie events and listeners.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-27
 */
public class EventBasedLifeCycleTest {

    private final Logger LOGGER = LoggerFactory.getLogger(EventBasedLifeCycleTest.class);

    /** Defines how many ms after test data is wired the actual auction will be run */
    private final long futureAuctionStartOffsetInMillis = 200;
    private final ChronoUnit unit = ChronoUnit.MILLIS;

    private static final AuctionState[] FULL_AUCTION_LIFECYLE_STATES = {
            CALL_RUNNING, CALL_STOPPED,
            PRICE_DETERMINATION_RUNNING, PRICE_DETERMINATION_STOPPED,
            ORDERBOOK_BALANCING_RUNNING, ORDERBOOK_BALANCING_STOPPED};

    private static final ContinuousTradingState[] EXPECTED_CONTINUOUS_TRADING_STATES = {
            ContinuousTradingState.RUNNING, ContinuousTradingState.STOPPED};


    @Test
    public void testScheduledPhaseWithSingleAuction() throws InterruptedException {
        assertPureAuctionTrading( Arrays.asList(FULL_AUCTION_LIFECYLE_STATES),
                new TradingFormRunType[]{OPENING_AUCTION}, multipleAuction(OPENING_AUCTION) );
    }


    @Test
    public void testScheduledPhaseWithThreeAuctions() throws InterruptedException {
        List<Enum> expectedStates = new ArrayList<>();
        expectedStates.addAll(Arrays.asList(FULL_AUCTION_LIFECYLE_STATES));
        expectedStates.addAll(Arrays.asList(FULL_AUCTION_LIFECYLE_STATES));
        expectedStates.addAll(Arrays.asList(FULL_AUCTION_LIFECYLE_STATES));
        TradingFormRunType[] tradingFormRunTypes = {OPENING_AUCTION, INTRADAY_AUCTION,CLOSING_AUCTION };
        assertPureAuctionTrading( expectedStates, tradingFormRunTypes, multipleAuction(tradingFormRunTypes));
    }


    @Test
    public void testScheduledPhasesForTomorrow_NoTradingExpected() throws InterruptedException {
        MonitoringStateChangedEventHandler stateChangedEventHandler =
                new MonitoringStateChangedEventHandler(0);
        DefaultNotificationListener notificationListener = new DefaultNotificationListener();
        notificationListener.setStateChangedEventHandler( stateChangedEventHandler );

        Auction auction = new Auction(notificationListener);

        AuctionTradingModel tradingModel = new AuctionTradingModel( new TradingModelProfile(), notificationListener, auction);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        TradingCalendar forYesterdayOnly = new TradingCalendar(yesterday);
        forYesterdayOnly.addAuction( createAuctionSchedule( startTradingTime(), TradingFormRunType.OPENING_AUCTION) );

        MarketRunner marketRunner = new MarketRunner( tradingModel, forYesterdayOnly );

        marketRunner.start( notificationListener );
        Thread.sleep(futureAuctionStartOffsetInMillis);
        marketRunner.stop();

        Assert.assertNull("No trading should have been started, since no session setup for today!",
                tradingModel.getTradingFormRunType());
        Assert.assertEquals("No trading scheduled for today!", 0, stateChangedEventHandler.getStateChangedEvents().size());
    }


    @Test
    public void testContTradingWithAuction() throws InterruptedException {

        List<Enum> allExpectedStates = new ArrayList<>();
        allExpectedStates.addAll( Arrays.asList(FULL_AUCTION_LIFECYLE_STATES));
        // before we switch to continuous trading we have to deactivate the auction
        allExpectedStates.add( AuctionState.INACTIVE );
        // switching into ContTrad
        allExpectedStates.addAll( Arrays.asList(EXPECTED_CONTINUOUS_TRADING_STATES));
        // and now again closing auction
        allExpectedStates.addAll( Arrays.asList(FULL_AUCTION_LIFECYLE_STATES));

        TradingCalendar tradingCalendar = createTradingCalendar();
        MonitoringStateChangedEventHandler stateChangedEventHandler =
                new MonitoringStateChangedEventHandler(allExpectedStates.size());
        DefaultNotificationListener notificationListener = new DefaultNotificationListener();
        notificationListener.setStateChangedEventHandler( stateChangedEventHandler );

        Auction auction = new Auction(notificationListener);
        ContinuousTrading continuousTrading = new ContinuousTrading(notificationListener);

        ContinuousTradingInterruptedByAuctions tradingModel = new ContinuousTradingInterruptedByAuctions(
                new TradingModelProfile(), notificationListener, continuousTrading, auction);

        MarketRunner marketRunner = new MarketRunner( tradingModel, tradingCalendar );

        marketRunner.start( notificationListener );
        stateChangedEventHandler.latch.await(1, TimeUnit.SECONDS);
        marketRunner.stop();

        List<Enum> actualStateChanges = stateChangedEventHandler.getStateChangedEvents().stream()
                .map(StateChangedEvent::getCurrent).collect(Collectors.toList());
        Assert.assertArrayEquals("Expected: " + allExpectedStates + " actual: " + actualStateChanges,
                allExpectedStates.toArray(),
                actualStateChanges.toArray());
    }


    @Test
    public void testRandomTimeTriggerWithinGivenOffsetRange() {
        int maxDuration = 100;

        for ( int min=0; min < maxDuration; min++ ) {
            LocalTime now = LocalTime.now();
            long nowMillis = now.get(ChronoField.MILLI_OF_DAY);

            AuctionSchedule schedule = AuctionSchedule.kickOff(TradingFormRunType.OPENING_AUCTION)
                    .startingCallPhaseAt(now)
                    .andLastForAtLeast(Duration.ofMillis(min))
                    .withRandomBufferBetween(Duration.ofMillis(maxDuration));
            List<TradingPhaseTrigger> triggers = schedule.getTriggers();

            Assert.assertEquals(4, triggers.size());

            // 2nd trigger stops call phase in certain intervall
            TradingPhaseTrigger endCallPhaseTrigger = triggers.get(1);
            Assert.assertEquals( TradingPhaseTrigger.InitiatorType.FIXED_TIME, endCallPhaseTrigger.getInitiatorType() );
            LocalTime triggerFixedTime = endCallPhaseTrigger.getFixedTime();
            long triggerStampMillis = triggerFixedTime.get(ChronoField.MILLI_OF_DAY);

            LOGGER.debug("now={} range=[{} -{}] triggerTime={}", nowMillis, min, maxDuration, triggerStampMillis);

            Assert.assertTrue(
                    String.format("triggerStampMillis=%d nowMillis+min=%d", triggerStampMillis, nowMillis + min),
                    triggerStampMillis >= nowMillis + min);

            Assert.assertTrue(
                    String.format("triggerStampMillis=%d nowMillis+max=%d", triggerStampMillis, nowMillis + maxDuration),
                    triggerStampMillis <= nowMillis + min + maxDuration);

        }
    }


    private void assertPureAuctionTrading( List<Enum> expectedStateChanges,
                                           TradingFormRunType[] expectedTradingFormRuntimes,
                                           TradingCalendar tradingCalendar ) throws InterruptedException {

        MonitoringStateChangedEventHandler stateChangedEventHandler =
                new MonitoringStateChangedEventHandler(expectedStateChanges.size());
        DefaultNotificationListener notificationListener = new DefaultNotificationListener();
        notificationListener.setStateChangedEventHandler( stateChangedEventHandler );

        Auction auction = new Auction(notificationListener);
        AuctionTradingModel auctionModel = new AuctionTradingModel(new TradingModelProfile(), notificationListener, auction);

        MarketRunner marketRunner = new MarketRunner( auctionModel, tradingCalendar );
        marketRunner.start( notificationListener );

        stateChangedEventHandler.latch.await();
        marketRunner.stop();

        List<Enum> actualStateChanges = stateChangedEventHandler.getStateChangedEvents().stream()
                .map(StateChangedEvent::getCurrent).collect(Collectors.toList());

        Assert.assertArrayEquals("Recorded state changes: " + actualStateChanges,
                expectedStateChanges.toArray(),
                actualStateChanges.toArray());
        Assert.assertArrayEquals( expectedTradingFormRuntimes, notificationListener.getTradingFormRunTypes().toArray() );
    }

    private TradingCalendar multipleAuction(TradingFormRunType ... tradingFormRunTypes) {
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
        tradingCalendar.addAuction( createAuctionSchedule(startTradingTime, runType));
        return tradingCalendar;
    }


    private TradingCalendar createTradingCalendar() {
        LocalTime startTradingTime = startTradingTime();
        LocalDate today = LocalDate.now();

        // opening auction
        TradingCalendar tradingCalendar = new TradingCalendar(today);
        tradingCalendar.addAuction(createAuctionSchedule(startTradingTime, TradingFormRunType.OPENING_AUCTION));

        // continuous trading (interrupted by auction)
        LocalTime startContinuousTradingTime = startTradingTime.plus(50, unit);
        LocalTime stopContinuousTradingTime = startContinuousTradingTime.plus(50, unit);
        tradingCalendar.addContinuousTrading(
                new ContinuousTradingSchedule(startContinuousTradingTime, stopContinuousTradingTime));

        // closing auction
        LocalTime startClosingAuctionTime = stopContinuousTradingTime.plus(300, unit);
        tradingCalendar.addAuction( createAuctionSchedule(startClosingAuctionTime, TradingFormRunType.CLOSING_AUCTION) );

        return tradingCalendar;
    }


    private LocalTime startTradingTime() {
        return LocalTime.now().plus(futureAuctionStartOffsetInMillis, unit);
    }


    private AuctionSchedule createAuctionSchedule(LocalTime startTradingTime, TradingFormRunType tradingFormRunType) {
        return AuctionSchedule.kickOff(tradingFormRunType)
                .startingCallPhaseAt(startTradingTime)
                .andLastForAtLeast(Duration.ofMillis(3))
                .withRandomBufferBetween(Duration.ofMillis(10));
    }


    private class MonitoringStateChangedEventHandler extends StateChangedEventHandler {
        final CountDownLatch latch;

        private MonitoringStateChangedEventHandler(int count) { latch = new CountDownLatch(count); }

        @Override
        public void handle(StateChangedEvent event) {
            super.handle(event);
            latch.countDown();
        }
    }
}
