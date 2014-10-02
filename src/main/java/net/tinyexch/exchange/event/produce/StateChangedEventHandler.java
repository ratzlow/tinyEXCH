package net.tinyexch.exchange.event.produce;

import net.tinyexch.exchange.event.MarketRunner;
import net.tinyexch.exchange.schedule.TradingCalendar;
import net.tinyexch.exchange.schedule.TradingPhaseTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static net.tinyexch.exchange.schedule.TradingPhaseTrigger.InitiatorType.WAIT_FOR_STATECHANGE;

/**
 * Is mapped in {@link net.tinyexch.exchange.event.NotificationListener} to take on handling of
 * {@link net.tinyexch.exchange.event.produce.StateChangedEvent}.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-25
 */
public class StateChangedEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateChangedEventHandler.class);

    private final List<StateChangedEvent> StateChangedEvents = new ArrayList<>();
    private final Deque<TradingPhaseTrigger> triggers = new ArrayDeque<>();
    private MarketRunner marketRunner;


    public void init( MarketRunner marketRunner, TradingCalendar tradingCalendar ) {
        this.marketRunner = marketRunner;
        Map<TradingPhaseTrigger.InitiatorType, List<TradingPhaseTrigger>> triggersByType =
                tradingCalendar.getTriggers().stream().collect(groupingBy(TradingPhaseTrigger::getInitiatorType));
        triggers.addAll(triggersByType.getOrDefault(WAIT_FOR_STATECHANGE, Collections.emptyList()));
    }


    public void handle( StateChangedEvent event ) {
        StateChangedEvents.add(event);

        // kick off life cycle transition in response to a life cycle state change
        TradingPhaseTrigger peekedTrigger = triggers.peek();
        if (peekedTrigger != null && peekedTrigger.getWaitFor() == event.getCurrent() ) {
            TradingPhaseTrigger trigger = triggers.pop();
            marketRunner.submit( trigger.getChangeStateEvent() );

        } else {
            LOGGER.info("No trigger found for state {} as we wait for {}", event.getCurrent(), peekedTrigger );
        }
    }

    public List<StateChangedEvent> getStateChangedEvents() { return StateChangedEvents; }
}
