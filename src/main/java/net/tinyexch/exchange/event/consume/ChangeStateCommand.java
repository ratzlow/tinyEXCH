package net.tinyexch.exchange.event.consume;

import net.tinyexch.exchange.trading.model.TradingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Transition to next state in life cycle of a trading form part of a trading model
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-26
 */
public class ChangeStateCommand implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeStateCommand.class);

    private final TradingModel tradingModel;
    private final ChangeStateEvent event;
    private final Consumer<TradingModel> stateChanger;


    public ChangeStateCommand(TradingModel tradingModel,
                              ChangeStateEvent event,
                              Consumer<TradingModel> stateChanger) {
        this.tradingModel = tradingModel;
        this.event = event;
        this.stateChanger = stateChanger;
    }


    @Override
    public void run() {
        try {
            // first check if we have to switch the trading form mode
            LOGGER.info("Start state change triggered by {}", event);
            event.getTradingFormRunType().ifPresent( tradingModel::setTradingFormRunType );
            stateChanger.accept( tradingModel );

        } catch (Exception e) {
            LOGGER.error("Cannot execute scheduled state change!", e);
        }
    }
}