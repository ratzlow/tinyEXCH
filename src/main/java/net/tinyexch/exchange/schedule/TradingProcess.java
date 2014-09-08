package net.tinyexch.exchange.schedule;

import net.tinyexch.exchange.trading.form.StateChangeListener;
import net.tinyexch.exchange.trading.form.TradingModelStateChanger;
import net.tinyexch.exchange.trading.model.AuctionTradingModel;
import net.tinyexch.exchange.trading.model.ContinuousTradingInterruptedByAuctions;
import net.tinyexch.exchange.trading.model.TradingFormRunType;
import net.tinyexch.exchange.trading.model.TradingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.synchronizedList;
import static java.util.stream.Collectors.groupingBy;
import static net.tinyexch.exchange.schedule.TradingPhaseTrigger.InitiatorType.FIXED_TIME;
import static net.tinyexch.exchange.schedule.TradingPhaseTrigger.InitiatorType.WAIT_FOR_STATECHANGE;

/**
 * Coordinate how to interact with an {@link net.tinyexch.ob.Orderbook} within a given
 * {@link net.tinyexch.exchange.trading.form.TradingForm} dependent on a state
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-03
 */
public class TradingProcess {
    private final static Logger LOGGER = LoggerFactory.getLogger(TradingProcess.class);
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    // mutable
    private final List<FiredStateChange> firedStateChanges = synchronizedList(new ArrayList<>());
    private final List<TradingFormRunType> firedTradingFormRunTypes = synchronizedList(new ArrayList<>());

    // no op handler
    private final Consumer<Enum> firedStateChangeConsumer;

    //
    // constructors
    //

    public TradingProcess( Consumer<Enum> firedStateChangeConsumer ) {
        this.firedStateChangeConsumer = firedStateChangeConsumer;
    }

    public TradingProcess() {
        firedStateChangeConsumer = newState -> {};
    }

    //
    // public API
    //

    public void startTrading( TradingCalendar tradingCalendar, AuctionTradingModel model ) {
        if ( !isTradingAllowedToday(tradingCalendar.getTradingDays()) ) { return; }

        tradingCalendar.getTradingFormSchedules().stream().forEach(schedule ->
                run(schedule.getInitializer(), schedule.getTriggers(), model));
    }


    public void startTrading( TradingCalendar tradingCalendar, ContinuousTradingInterruptedByAuctions model ) {
        if ( !isTradingAllowedToday(tradingCalendar.getTradingDays()) ) { return; }

        tradingCalendar.getTradingFormSchedules().stream().forEach(schedule ->
                run(schedule.getInitializer(), schedule.getTriggers(), model));
    }


    private void run(TradingFormInitializer initializer, List<TradingPhaseTrigger> triggers,
                     TradingModel tradingModel) {

        TradingPhaseTrigger firstTrigger = triggers.get(0);
        if ( firstTrigger.getInitiatorType() != FIXED_TIME) {
            throw new SchedulerException( "Trading must start at a fixed time! " + firstTrigger );
        }

        // first register state tracking listeners
        final Map<TradingPhaseTrigger.InitiatorType, List<TradingPhaseTrigger>> triggersByType =
                triggers.stream().collect(groupingBy(TradingPhaseTrigger::getInitiatorType));

        List<StateChangeListener> waitforStateChangeListeners =
                triggersByType.getOrDefault(WAIT_FOR_STATECHANGE, Collections.emptyList()).stream()
                        .map(trigger -> createMoveStateListener(trigger.getWaitFor(), trigger.getStateChanger(), tradingModel))
                        .collect(Collectors.toList());

        List<StateChangeListener> runtimeListeners = new ArrayList<>();
        // first listener is a life cycle monitor
        runtimeListeners.add( createStateChangedListener() );
        // further listeners kick off transitions on demand
        runtimeListeners.addAll(waitforStateChangeListeners);

        // now kick off all triggers to be run in different thread
        LocalTime now = LocalTime.now();
        if ( triggersByType.containsKey(FIXED_TIME)) {
            List<TradingPhaseTrigger> timedTriggers = triggersByType.get(FIXED_TIME);
            for (int i = 0; i < timedTriggers.size(); i++) {
                TradingPhaseTrigger trigger = timedTriggers.get(i);
                TradingModelStateChanger stateChanger = trigger.getStateChanger();
                Duration duration = trigger.getDurationToFire(now);
                long delayMillis = duration.getNano() / 1_000_000;
                assert (delayMillis >= 0);
                boolean firstCmd = i == 0;
                final Runnable scheduledListener = createScheduledListener(tradingModel, stateChanger, firstCmd,
                        initializer, runtimeListeners);
                LOGGER.info("Adding scheduled state listener to fire in {} ms", delayMillis);
                executorService.schedule(
                        scheduledListener,
                        delayMillis, TimeUnit.MILLISECONDS
                );
            }
        }
    }

    private Runnable createScheduledListener(TradingModel tradingModel,
                                             TradingModelStateChanger stateChanger,
                                             boolean firstCmd,
                                             TradingFormInitializer initializer,
                                             List<StateChangeListener> runtimeListeners) {
        return () -> {
            try {

                if (firstCmd) {
                    LOGGER.info("Initialize TradingForm ...");
                    tradingModel.init(initializer, runtimeListeners);
                    firedTradingFormRunTypes.add(initializer.getTradingFormRuntype());
                }

                LOGGER.info("Start scheduled state change");
                stateChanger.transition(tradingModel);
            } catch (Exception e) {
                LOGGER.error("Cannot execute scheduled state change!", e);
            }
        };
    }

    public List<FiredStateChange> getFiredStateChanges() {
        return firedStateChanges;
    }

    public List<TradingFormRunType> getFiredTradingFormRunTypes() {
        return firedTradingFormRunTypes;
    }

    //------------------------------------------------------------------------------------------------------------------
    // impl details
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Create listener that will initiate a transition as soon as a new state we wait for was switched to
     * @param waitFor kick off transition when this state is encountered
     * @param stateChanger strategy that controls the transition
     * @return listener on a trading form
     */
    // TODO (FRa) : (FRa) : consolidate error handling
    private StateChangeListener createMoveStateListener( Enum waitFor, TradingModelStateChanger stateChanger,
                                                         TradingModel tradingModel ) {
        return state -> {
            try {
                LOGGER.debug("Fired listener as state changed to {}", state);
                if (waitFor == state) {
                    stateChanger.transition(tradingModel);
                }
            } catch (Exception e) {
                LOGGER.error("Could not perform transition!", e);
            }
        };
    }

    /**
     * @return all state changes will be recorded
     */
    private StateChangeListener createStateChangedListener() {
        return state -> {
            FiredStateChange stateChange = new FiredStateChange(Instant.now(), state);
            LOGGER.info("added " + stateChange );
            firedStateChanges.add(stateChange);
            firedStateChangeConsumer.accept(state);
        };
    }

    /**
     * Check if the trading process might be kicked off today.
     * @param permittedTradingDays containing the allowed trading schedule
     * @return true ... trading allowed today
     */
    private boolean isTradingAllowedToday(List<LocalDate> permittedTradingDays) {
        LocalDate today = LocalDate.now();
        return permittedTradingDays.stream().anyMatch(date -> today.compareTo(date) == 0);
    }

    /**
     * Record of a state change
     */
    public static class FiredStateChange {
        private final Instant timestamp;
        private final Enum newState;

        private FiredStateChange(Instant timestamp, Enum newState) {
            this.timestamp = timestamp;
            this.newState = newState;
        }

        public Instant getTimestamp() { return timestamp; }

        public Enum getNewState() { return newState; }

        @Override
        public String toString() {
            return "FiredStateChange{" +
                    "timestamp=" + timestamp +
                    ", newState=" + newState +
                    '}';
        }
    }
}