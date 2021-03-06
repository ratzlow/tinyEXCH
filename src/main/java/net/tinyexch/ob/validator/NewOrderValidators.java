package net.tinyexch.ob.validator;

import net.tinyexch.ob.ErrorCode;
import net.tinyexch.ob.RejectReason;
import net.tinyexch.order.*;

import java.time.LocalDate;
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

        @Override
        public Optional<ErrorCode> validate(Order order) {
            TimeInForce timeInForce = order.getTimeInForce();
            boolean invalidGtdOrder = timeInForce == TimeInForce.GTD && !isValidGtd(order);
            boolean invalidDayOrder = timeInForce == TimeInForce.DAY && !isValidDay(order);

            return invalidGtdOrder || invalidDayOrder ?  Optional.of(createErrorCode(order)) : Optional.empty();
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
            return new ErrorCode(ErrorCode.Type.REJECT, order, "GTD order is not within valid range", RejectReason.GTD);
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


    /**
     * SMO is only allowed in auctions so that you can use the new TradingSessionSubID to be even more explicit.
     * DiscretionLimitType=0=Or better identifies a normal discretionary order as opposed to a SMO.
     * Strike Match Orders are only valid for the Closing Auction of the respective trading day and are deleted during
     * the end of day processing.
     */
    public final NewOrderValidator strikeMatchOrderTypeCheck = new NewOrderValidator() {
        @Override
        public Optional<ErrorCode> validate(Order order) {
            TimeInForce tif = order.getTimeInForce();
            boolean validTodayOnly = tif == TimeInForce.DAY || (tif == TimeInForce.GTD && isToday(order.getExpirationDate()) );

            boolean ok = order.getOrderType() == OrderType.STRIKE_MATCH &&
                         order.getTradingSessionSubID() == TradingSessionSubID.ClosingOrClosingAuction &&
                         order.getDiscretionLimitType() == DiscretionLimitType.OR_WORSE &&
                         order.getStopPrice() > 0 &&
                         validTodayOnly;

            return ok ? Optional.<ErrorCode>empty() : Optional.of(new ErrorCode(ErrorCode.Type.REJECT, order,
                    "Insufficient attributes for SMO", RejectReason.ORDER_TYPE));
        }

        boolean isToday( LocalDateTime dateTime ) {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            return tomorrow.isAfter(dateTime.toLocalDate());
        }
    };

    public final Stream<NewOrderValidator> newOrderValidators = Stream.of(minSizeCheck, gtdCheck, orderTypeCheck,
            strikeMatchOrderTypeCheck);
}
