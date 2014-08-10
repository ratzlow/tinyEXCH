package net.tinyexch.exchange.trading.form.auction;

import net.tinyexch.exchange.trading.form.TradingFormStateChanger;

/**
 * To switch an auction trading model's auction through it's life cycle.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-03
 */
public enum AuctionStateChanger implements TradingFormStateChanger<AuctionProvider> {
    START_CALL {
        @Override
        public void transition(AuctionProvider provider) {
            provider.getAuction().startCallPhase();
        }
    },

    STOP_CALL {
        @Override
        public void transition(AuctionProvider provider) {
            provider.getAuction().stopCallPhase();
        }
    },

    START_PRICEDETERMINATION {
        @Override
        public void transition(AuctionProvider provider) {
            provider.getAuction().determinePrice();
        }
    },

    START_ORDERBOOK_BALANCING {
        @Override
        public void transition(AuctionProvider provider) {
            provider.getAuction().balanceOrderbook();
        }
    }
}
