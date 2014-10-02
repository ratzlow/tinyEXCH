package net.tinyexch.exchange.trading.form.continuous;

import net.tinyexch.exchange.event.NotificationListener;
import net.tinyexch.exchange.trading.form.TradingForm;
import net.tinyexch.ob.SubmitType;
import net.tinyexch.ob.price.safeguard.VolatilityInterruptionEmitter;
import net.tinyexch.order.Order;
import net.tinyexch.order.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.singleton;
import static net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState.RUNNING;
import static net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState.STOPPED;

/**
 * Order/quote driven trading form where price is continually derived based on incoming orders and quotes. Opposite
 * orderbook sides are attempted to match immediately with every incoming order/quote.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-01
 */
public class ContinuousTrading extends TradingForm<ContinuousTradingState> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContinuousTrading.class);

    private boolean volatilityInterrupted = false;

    /** default: no op implementation */
    private VolatilityInterruptionEmitter volatilityInterruptionEmitter = VolatilityInterruptionEmitter.NO_OP_EMITTER;


    public ContinuousTrading( NotificationListener notificationListener ) {
        super(notificationListener);
    }

    @Override
    protected Map<ContinuousTradingState, Set<ContinuousTradingState>> getAllowedTransitions() {
        Map<ContinuousTradingState, Set<ContinuousTradingState>>allowedTransitions = new EnumMap<>(ContinuousTradingState.class);
        allowedTransitions.put(STOPPED, singleton(RUNNING));
        allowedTransitions.put(RUNNING, singleton(STOPPED));
        return allowedTransitions;
    }


    public void submit(Order order, SubmitType submitType) {

        Optional<Trade> submitResult = getOrderbook().submit(order, submitType);

        // TODO (FRa) : (FRa) : is this applicable?! too functional?
        // check for volatility interruptions apply for auction and continuous trading
        volatilityInterrupted = submitResult
                .map(trade -> volatilityInterruptionEmitter.checkIndicativePrice( trade.getExecutionPrice() ))
                .orElse(false);
    }

    @Override
    public ContinuousTradingState getDefaultState() { return STOPPED; }

    @Override
    public void close() {
        transitionTo( ContinuousTradingState.STOPPED );
    }

    public void start() {
        transitionTo( ContinuousTradingState.RUNNING);
    }

    @Override
    protected Logger getLogger() { return LOGGER; }


    public void register( VolatilityInterruptionEmitter emitter ) {
        volatilityInterruptionEmitter = emitter;
    }

    public void clearVolatilityInterruption() { volatilityInterrupted = false; }
}
