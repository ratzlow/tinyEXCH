package net.tinyexch.exchange.event.consume;

import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.auction.AuctionProvider;
import net.tinyexch.exchange.trading.form.auction.AuctionState;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState;
import net.tinyexch.exchange.trading.model.ContinuousTradingInterruptedByAuctions;
import net.tinyexch.exchange.trading.model.TradingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create commands that can invoke appropriate actions on a given event. Commands work as the event handler.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-27
 */
public class ConsumerCommandFactory {

    private final static Logger LOGGER = LoggerFactory.getLogger(ConsumerCommandFactory.class);

    private TradingModel tradingModel;
    private Auction auction;
    private ContinuousTrading continuousTrading;


    public ConsumerCommandFactory(TradingModel tradingModel) {
        this.tradingModel = tradingModel;
        this.auction = getAuction( tradingModel );
        this.continuousTrading = getContinuousTrading( tradingModel );
    }

    //
    // public API
    //

    public <T> Runnable create(T event) {

        Runnable cmd = null;
        if ( event instanceof ChangeStateEvent ) {
            ChangeStateEvent e = (ChangeStateEvent) event;
            Enum targetState = e.getTargetState();
            if ( targetState == AuctionState.CALL_RUNNING ) {
                cmd = new ChangeStateCommand( tradingModel, e, model -> auction.startCallPhase() );

            } else if (targetState == AuctionState.CALL_STOPPED ) {
                cmd = new ChangeStateCommand(tradingModel, e, model -> auction.stopCallPhase() );

            } else if (targetState == AuctionState.PRICE_DETERMINATION_RUNNING ) {
                cmd = new ChangeStateCommand(tradingModel, e, model -> auction.determinePrice() );

            } else if (targetState == AuctionState.ORDERBOOK_BALANCING_RUNNING ) {
                cmd = new ChangeStateCommand(tradingModel, e, model -> auction.balanceOrderbook() );

            } else if (targetState == ContinuousTradingState.RUNNING ) {
                cmd = new ChangeStateCommand(tradingModel, e, model -> {
                    auction.close();
                    continuousTrading.start();
                });

            } else if (targetState == ContinuousTradingState.STOPPED ) {
                cmd = new ChangeStateCommand(tradingModel, e, model -> continuousTrading.close() );
            } else {
                LOGGER.warn("No cmd mapped to event {}", event);
            }

        } else {
            throw new IllegalArgumentException("No Handler mapped to " + event);
        }

        return cmd;
    }


    private Auction getAuction(TradingModel tradingModel) {
        Auction auction = null;
        if ( tradingModel instanceof AuctionProvider ) {
            auction = ((AuctionProvider) tradingModel).getAuction();

        } else {
            LOGGER.info("Cannot give access to auction as the current trading model " +
                    "doesn't support it! " + this.tradingModel.getClass().getSimpleName());
        }

        return auction;
    }


    private ContinuousTrading getContinuousTrading(TradingModel tradingModel) {
        ContinuousTrading continuousTrading = null;
        if ( tradingModel instanceof ContinuousTradingInterruptedByAuctions ) {
            continuousTrading = ((ContinuousTradingInterruptedByAuctions) tradingModel).getContinuousTrading();

        } else {
            LOGGER.info("Cannot give access to continuous trading as the current trading model " +
                    "doesn't support it! " + this.tradingModel.getClass().getSimpleName());
        }

        return continuousTrading;
    }
}
