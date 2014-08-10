package net.tinyexch.exchange.trading.form.continuous;

import net.tinyexch.exchange.trading.form.TradingFormStateChanger;
import net.tinyexch.exchange.trading.form.auction.Auction;
import net.tinyexch.exchange.trading.form.auction.AuctionState;

/**
 * Flip through the life cycle of an auction and continuous trading.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-04
 */
public class ContinuousTradingAuctionsStateChanger
        implements TradingFormStateChanger<ContinuousTradingAuctionsProvider> {

    // TODO (FRa) : (FRa) : rewrite the values with Optional type
    private final AuctionState auctionState;
    private final ContinuousTradingState continuousTradingState;

    public ContinuousTradingAuctionsStateChanger( AuctionState auctionState,
                                                  ContinuousTradingState continuousTradingState ) {
        // verify we have a valid combination of states
        validateParams(auctionState, continuousTradingState);

        this.auctionState = auctionState;
        this.continuousTradingState = continuousTradingState;
    }

    @Override
    public void transition(ContinuousTradingAuctionsProvider provider) {

        Auction auction = provider.getAuction();
        boolean activeAuction = auction.getCurrentState().isActive();

        ContinuousTrading continuousTrading = provider.getContinuousTrading();
        boolean activeContTrading = continuousTrading.getCurrentState().isActive();

        // make sure we don't have at this time 2 continuous trading forms running
        if ( activeAuction && activeContTrading ) {
            throw new IllegalStateException("There must not be 2 active trading forms at the same time!");
        }

        /*
        // first deal with the active session then with the inactive
        if (activeAuction) {
            auction.transitionTo(auctionState);
            continuousTrading.transitionTo(continuousTradingState);

        // continuous trading must be active
        } else {
            continuousTrading.transitionTo(continuousTradingState);
            auction.transitionTo(auctionState);
        }
        */
    }

    private void validateParams(AuctionState auctionState, ContinuousTradingState continuousTradingState) {
        if ( auctionState == null && continuousTradingState == null ) {
            throw new IllegalStateException("One of the trading forms must have a new target state assigned");

        } else if ( auctionState != null && continuousTradingState != null &&
                auctionState.isActive() && continuousTradingState.isActive() ) {
            throw new IllegalStateException("You can only run one active trading forms!");
        }
    }
}