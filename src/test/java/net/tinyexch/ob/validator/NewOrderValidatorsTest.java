package net.tinyexch.ob.validator;

import net.tinyexch.ob.ErrorCode;
import net.tinyexch.ob.RejectReason;
import net.tinyexch.order.Order;
import net.tinyexch.order.OrderType;
import net.tinyexch.order.Side;
import net.tinyexch.order.TimeInForce;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

/**
 * Test interaction and state of the OB.
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-27
 */
public class NewOrderValidatorsTest {

    private EnumSet<OrderType> acceptedOrderTypes = EnumSet.allOf(OrderType.class);
    private int sequence = 0;


    @Test
    public void testMinSizeCheck() {
        NewOrderValidators check_1 = new NewOrderValidators(1, 1, 360, acceptedOrderTypes);
        Assert.assertEquals(
                ErrorCode.Type.REJECT,
                check_1.minSizeCheck.validate( newOrder(Side.SELL, 0)).get().type );
        Assert.assertFalse(
                check_1.minSizeCheck.validate(newOrder(Side.SELL, 1)).isPresent());

        // no min sizes set, so everything can be submitted
        NewOrderValidators check_2 = new NewOrderValidators(0, 0, 360, acceptedOrderTypes);
        Assert.assertFalse(check_2.minSizeCheck.validate(newOrder(Side.SELL, 0)).isPresent());
        Assert.assertFalse(check_2.minSizeCheck.validate(newOrder(Side.SELL, 1)).isPresent());


        // deal with different min order size and trading unit
        NewOrderValidators check_3 = new NewOrderValidators(10, 1, 360, acceptedOrderTypes);
        ErrorCode submit_3_1 = check_3.minSizeCheck.validate(newOrder(Side.SELL, 9)).get();
        Assert.assertEquals( ErrorCode.Type.REJECT, submit_3_1.type );
        Assert.assertEquals(RejectReason.MIN_SIZE, submit_3_1.rejectReason.get());
        Assert.assertFalse( check_3.minSizeCheck.validate(newOrder(Side.SELL, 10)).isPresent());
    }


    @Test
    public void testGtd() {
        int maxExpirationDayOffset = 359;
        NewOrderValidators check = new NewOrderValidators(1, 1, maxExpirationDayOffset,
                acceptedOrderTypes);
        LocalDateTime now = LocalDateTime.now();

        Order gtdOrderValidToday = new Order(newOrderID()).setTimeInForce(TimeInForce.GTD)
                .setExpirationDate(now.truncatedTo(ChronoUnit.DAYS));
        Assert.assertFalse( check.gtdCheck.validate(gtdOrderValidToday).isPresent());


        Order dayOrderValidToday = new Order(newOrderID()).setTimeInForce(TimeInForce.DAY)
                .setExpirationDate(now.truncatedTo(ChronoUnit.DAYS));
        Assert.assertFalse( check.gtdCheck.validate(dayOrderValidToday).isPresent());


        Order dayOrderInvalidTomorrow = new Order(newOrderID()).setTimeInForce(TimeInForce.DAY)
                .setExpirationDate(now.plusDays(1).truncatedTo(ChronoUnit.DAYS));
        assertFailedGtdValidation( check, dayOrderInvalidTomorrow);


        Order orderAtLastAcceptableDate = new Order(newOrderID()).setTimeInForce(TimeInForce.GTD)
                .setExpirationDate(now.plusDays(maxExpirationDayOffset));
        Assert.assertFalse( check.gtdCheck.validate(orderAtLastAcceptableDate).isPresent() );


        Order orderInPast = new Order(newOrderID()).setExpirationDate(now.minusDays(1));
        assertFailedGtdValidation(check, orderInPast);


        Order orderTooFarInFuture = new Order(newOrderID()).setTimeInForce(TimeInForce.GTD)
                .setExpirationDate(now.plusDays(maxExpirationDayOffset + 1));
        assertFailedGtdValidation(check, orderTooFarInFuture);
    }


    private void assertFailedGtdValidation(NewOrderValidators check, Order order) {
        ErrorCode errorCode = check.gtdCheck.validate(order).get();
        Assert.assertEquals(ErrorCode.Type.REJECT, errorCode.type);
        Assert.assertEquals(RejectReason.GTD, errorCode.rejectReason.get());
    }

    private Order newOrder(Side side, int size ) {
        return new Order(newOrderID()).setSide(side).setOrderQty(size);
    }

    private String newOrderID() {
        return Integer.valueOf(++sequence).toString();
    }
}
