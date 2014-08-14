package net.tinyexch.exchange.trading.model;

import net.tinyexch.exchange.trading.form.TradingFormProvider;
import net.tinyexch.exchange.trading.form.TradingFormStateChanger;
import net.tinyexch.order.Order;

/**
 * Specifies how the trading process for a given security should be executed. It defines the sequence and kind of
 * {@link net.tinyexch.exchange.trading.form.TradingForm}
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-26
 * @link chap 2, 2.
 */
public abstract class TradingModel<P extends TradingFormProvider,
                                   C extends TradingFormStateChanger<P>> {

    private final TradingModelProfile profile;

    //--------------------------------------------------------------------------------
    // constructors
    //--------------------------------------------------------------------------------

    protected TradingModel(TradingModelProfile profile) {
        this.profile = profile;
    }


    //--------------------------------------------------------------------------------
    // pub API
    //--------------------------------------------------------------------------------

    public abstract void moveTo(C stateChanger);

    public void enter( Order order) {
        // TODO (FRa) : (FRa) : activate filters and return notifcation ACK/REJ to client
        // Stream<NewOrderValidator> newOrderValidators = profile.getNewOrderValidators().newOrderValidators;
        //orderbook.submit(order);
    }

    /**
     * The order in the orderbook keeps the same ID. Only certain attributes are changed in place.
     *
     * @param order the order representing with the updated values
     */
    public void modify( Order order ) {
        // TODO (FRa) : (FRa) : implement
    }


    /**
     * Given order should be canceled in the orderbook and removed from it.
     *
     * @param order the order representing with the updated values
     */
    public void cancel( Order order ) {
        // TODO (FRa) : (FRa) : implement
    }
}
