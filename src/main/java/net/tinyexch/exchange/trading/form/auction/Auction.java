package net.tinyexch.exchange.trading.form.auction;

import net.tinyexch.exchange.trading.form.StateChangeListener;
import net.tinyexch.exchange.trading.form.TradingForm;
import net.tinyexch.ob.OrderReceiver;
import net.tinyexch.ob.SubmitType;
import net.tinyexch.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.Collections.singleton;
import static net.tinyexch.exchange.trading.form.auction.AuctionState.*;

/**
 * This mode resembles reflects an auction technical wise. A provided OB will be balanced after the best price
 * could be determined.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-02
 */
public class Auction extends TradingForm<AuctionState> implements OrderReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(Auction.class);

    private final CallPhase callPhase;
    private final PriceDeterminationPhase priceDeterminationPhase;
    private final OrderbookBalancingPhase orderbookBalancingPhase;

    //-------------------------------------------------------------------------------------
    // constructors
    //-------------------------------------------------------------------------------------

    public Auction( List<StateChangeListener<AuctionState>> stateChangeListeners,
                    CallPhase callPhase,
                    PriceDeterminationPhase priceDeterminationPhase,
                    OrderbookBalancingPhase orderbookBalancingPhase ) {

        super(stateChangeListeners);
        this.callPhase = callPhase;
        this.priceDeterminationPhase = priceDeterminationPhase;
        this.orderbookBalancingPhase = orderbookBalancingPhase;
    }

    public Auction() {
        super();
        callPhase = order -> LOGGER.info("Accepted order: {}", order);
        priceDeterminationPhase = () -> {};
        orderbookBalancingPhase = () -> {};
    }

    //-------------------------------------------------------------------------------------
    // different auction phases
    //-------------------------------------------------------------------------------------


    // TODO (FRa) : (FRa) : ensure validations are called upfront; maybe add check to auction phase upfront as well
    @Override
    public void submit( Order order, SubmitType submitType ) {
        if ( getCurrentState() != CALL_RUNNING ) {
            String msg = "Call phase not opened so cannot accept order! Current state is = " + getCurrentState();
            throw new AuctionException(msg);
        }

        callPhase.accept( order );
    }
    public void startCallPhase() {
        transitionTo( CALL_RUNNING );
        orderbook.closePartially();
    }

    public void stopCallPhase() {
        transitionTo( CALL_STOPPED );
    }

    public void determinePrice() {
        transitionTo( PRICE_DETERMINATION_RUNNING );
        orderbook.close();
        priceDeterminationPhase.determinePrice();
        transitionTo( PRICE_DETERMINATION_STOPPED );
    }

    public void balanceOrderbook() {
        transitionTo( ORDERBOOK_BALANCING_RUNNING );
        orderbook.close();
        orderbookBalancingPhase.balance();
        transitionTo( ORDERBOOK_BALANCING_STOPPED );
    }


    @Override
    protected Map<AuctionState, Set<AuctionState>> getAllowedTransitions() {
        Map<AuctionState, Set<AuctionState>> transitions = new EnumMap<>(AuctionState.class);
        transitions.put(INACTIVE, EnumSet.of(CALL_RUNNING, PRICE_DETERMINATION_RUNNING, ORDERBOOK_BALANCING_RUNNING) );
        transitions.put(CALL_RUNNING, singleton(CALL_STOPPED));
        transitions.put(CALL_STOPPED, EnumSet.of(INACTIVE, PRICE_DETERMINATION_RUNNING));
        transitions.put(PRICE_DETERMINATION_RUNNING, singleton(PRICE_DETERMINATION_STOPPED) );
        transitions.put(PRICE_DETERMINATION_STOPPED, EnumSet.of(INACTIVE, ORDERBOOK_BALANCING_RUNNING));
        transitions.put(ORDERBOOK_BALANCING_RUNNING, singleton(ORDERBOOK_BALANCING_STOPPED) );
        transitions.put(ORDERBOOK_BALANCING_STOPPED, singleton(INACTIVE) );

        return transitions;
    }

    @Override
    public AuctionState getDefaultState() { return INACTIVE; }
}
