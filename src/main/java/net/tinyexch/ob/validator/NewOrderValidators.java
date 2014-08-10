package net.tinyexch.ob.validator;

import net.tinyexch.ob.ErrorCode;
import net.tinyexch.ob.RejectReason;
import net.tinyexch.order.Order;
import net.tinyexch.order.OrderType;
import net.tinyexch.order.TimeInForce;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Companion class for {@link net.tinyexch.ob.validator.NewOrderValidator}. Contains access to all specific validations.
 * // TODO (FRa) : (FRa) : make all classes final static so no recreation of results is needed. Maybe even as enums
 *  * // TODO (FRa) : (FRa) : write collector for list of rule validations
 * // TODO (FRa) : (FRa) : optimize: move validations be
 *                 // TODO (FRa) : (FRa) : interpolate concrete error msg only lazily: show actual vs. expected valus
 *
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-29
 */
public final class NewOrderValidators {

    //------------------------------------------------------------------------------------------------------------------
    // config parameters
    //------------------------------------------------------------------------------------------------------------------

    /**
     * That can be placed into the book.
     *
     * @link chap 2, 4.
     */
    private final int minimumOrderSize;

    /**
     * Minimum qty that can be matched.
     * // TODO (FRa) : (FRa) : respect this param in the match engine.
     *
     * @link chap 2, 4.
     */
    private final int minimumTradableUnit;

    private final int effectiveMinSize;

    /**
     * Usually described as 'T+359', where 'T'=today. Means order is valid for 360 days
     */
    private final int maxExpirationDayOffset;

    /**
     * These order types can be submitted to a given orderbook. This is dependent on the trading model and market model.
     */
    private final Set<OrderType> acceptedOrderTypes;


    //------------------------------------------------------------------------------------------------------------------
    // constructors
    //------------------------------------------------------------------------------------------------------------------

    public NewOrderValidators() {
        this(1, 1, 359, EnumSet.allOf(OrderType.class));
    }

    public NewOrderValidators(int minimumOrderSize, int minimumTradableUnit, int maxExpirationDayOffset, Set<OrderType> acceptedOrderTypes) {
        this.minimumOrderSize = minimumOrderSize;
        this.minimumTradableUnit = minimumTradableUnit;
        this.effectiveMinSize = Math.max(minimumOrderSize, minimumTradableUnit);
        this.maxExpirationDayOffset = maxExpirationDayOffset;
        this.acceptedOrderTypes = acceptedOrderTypes;
    }

    //------------------------------------------------------------------------------------------------------------------
    // instances of the concrete validators
    //------------------------------------------------------------------------------------------------------------------

    public final NewOrderValidator minSizeCheck = new NewOrderValidator() {
        @Override
        public Optional<ErrorCode> validate(Order order) {
            final Optional<ErrorCode> result;
            if ( order.getOrderQty() < effectiveMinSize ) {
                result = Optional.of( new ErrorCode(ErrorCode.Type.REJECT, order, "Less than allowed min size!", RejectReason.MIN_SIZE) );
            } else {
                result = Optional.empty();
            }
            return result;
        }
    };


    /**
     * @link chap 2, 14.
     */
    public final NewOrderValidator gtdCheck = new NewOrderValidator() {
        final String hint = "GTD order is not within valid range";

        @Override
        public Optional<ErrorCode> validate(Order order) {
            Optional<ErrorCode> result = Optional.empty();
            TimeInForce timeInForce = order.getTimeInForce();

            if ( (timeInForce == TimeInForce.GTD && !isValidGtd(order)) ||
                 (timeInForce == TimeInForce.DAY && !isValidDay(order)) ) {

                result = Optional.of(createErrorCode(order));
            }

            return result;
        }

        private boolean isValidDay(Order order) {
            LocalDateTime submittedExpirationDate = order.getExpirationDate().truncatedTo(ChronoUnit.DAYS);
            return today().isEqual(submittedExpirationDate);
        }

        private boolean isValidGtd(Order order) {
            LocalDateTime expirationDate = order.getExpirationDate().truncatedTo(ChronoUnit.DAYS);
            LocalDateTime today = today();
            LocalDateTime yesterday = today.minusDays(1);
            LocalDateTime maxExpirationDate = today.plusDays(maxExpirationDayOffset+1);
            return expirationDate.isAfter(yesterday) && expirationDate.isBefore(maxExpirationDate);
        }

        private LocalDateTime today() {
            return LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        }

        private ErrorCode createErrorCode(Order order) {
            return new ErrorCode(ErrorCode.Type.REJECT, order, hint, RejectReason.GTD);
        }
    };


    public final NewOrderValidator orderTypeCheck = new NewOrderValidator() {
        @Override
        public Optional<ErrorCode> validate(Order order) {
            Optional<ErrorCode> code = Optional.empty();
            if ( !acceptedOrderTypes.contains(order.getOrderType()) ) {
                code = Optional.of(new ErrorCode(ErrorCode.Type.REJECT, order,
                        "Given order type not support in this trading phase", RejectReason.ORDER_TYPE));
            }
            return code;
        }
    };



    public final Stream<NewOrderValidator> newOrderValidators = Stream.of(minSizeCheck, gtdCheck, orderTypeCheck);
}
