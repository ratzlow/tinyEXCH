package net.tinyexch.exchange.trading.model;

import net.tinyexch.exchange.runtime.MarketNotification;
import net.tinyexch.exchange.runtime.NotificationListener;
import net.tinyexch.ob.match.MatchEngine;
import net.tinyexch.ob.price.safeguard.VolatilityInterruptionEmitter;
import net.tinyexch.ob.validator.NewOrderValidators;
import net.tinyexch.order.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Define how a concrete trading model should operate at a given time.
 * E.g. an EOD auction might execute or accept different set of orders than on an intraday auction.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-01
 */
public class TradingModelProfile {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradingModelProfile.class);

    /**
     * Allowed order types that can be entered in the book in this model.
     */
    private Set<OrderType> supportedOrderTypes = EnumSet.allOf(OrderType.class);

    /**
     * Some order types must only be considered in specific trading models.
     */
    private Set<OrderType> executableOrderTypes = EnumSet.allOf(OrderType.class);

    private NewOrderValidators newOrderValidators = new NewOrderValidators();

    private VolatilityInterruptionEmitter volatilityInterruptionEmitter = VolatilityInterruptionEmitter.NO_OP_EMITTER;

    /** No Op matching */
    private MatchEngine auctionMatchEngine = order -> Optional.empty();

    /** No Op matching */
    private MatchEngine continuousTradingMatchEngine = order -> Optional.empty();

    /** All messages emitted by a market will be transmitted to the notification consumer */
    private NotificationListener notificationListener = notification -> LOGGER.warn("No notification listener set!");


    //-----------------------------------------------------------------------------
    // getter & setter
    //-----------------------------------------------------------------------------


    public NotificationListener getNotificationListener() {
        return notificationListener;
    }

    public void setNotificationListener(NotificationListener notificationListener) {
        this.notificationListener = notificationListener;
    }

    public MatchEngine getAuctionMatchEngine() {
        return auctionMatchEngine;
    }

    public void setAuctionMatchEngine(MatchEngine auctionMatchEngine) {
        this.auctionMatchEngine = auctionMatchEngine;
    }

    public MatchEngine getContinuousTradingMatchEngine() {
        return continuousTradingMatchEngine;
    }

    public void setContinuousTradingMatchEngine(MatchEngine continuousTradingMatchEngine) {
        this.continuousTradingMatchEngine = continuousTradingMatchEngine;
    }

    public Set<OrderType> getSupportedOrderTypes() {
        return supportedOrderTypes;
    }

    public void setSupportedOrderTypes(Set<OrderType> supportedOrderTypes) {
        this.supportedOrderTypes = supportedOrderTypes;
    }

    public Set<OrderType> getExecutableOrderTypes() {
        return executableOrderTypes;
    }

    public void setExecutableOrderTypes(Set<OrderType> executableOrderTypes) {
        this.executableOrderTypes = executableOrderTypes;
    }

    public NewOrderValidators getNewOrderValidators() {
        return newOrderValidators;
    }

    public void setNewOrderValidators(NewOrderValidators newOrderValidators) {
        this.newOrderValidators = newOrderValidators;
    }

    public VolatilityInterruptionEmitter getVolatilityInterruptionEmitter() {
        return volatilityInterruptionEmitter;
    }

    public void setVolatilityInterruptionEmitter(VolatilityInterruptionEmitter volatilityInterruptionEmitter) {
        this.volatilityInterruptionEmitter = volatilityInterruptionEmitter;
    }
}
