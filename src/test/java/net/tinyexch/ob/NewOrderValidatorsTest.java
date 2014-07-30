package net.tinyexch.ob;

import net.tinyexch.ob.validator.NewOrderValidators;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Test interaction and state of the OB.
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-27
 */
public class NewOrderValidatorsTest {

    @Test
    public void testMinSizeCheck() {
        NewOrderValidators check_1 = new NewOrderValidators(1, 1, 360);
        Assert.assertEquals(
                ErrorCode.Type.REJECT,
                check_1.minSizeCheck.validate( newOrder(Side.SELL, 0)).get().type );
        Assert.assertFalse(
                check_1.minSizeCheck.validate(newOrder(Side.SELL, 1)).isPresent());

        // no min sizes set, so everything can be submitted
        NewOrderValidators check_2 = new NewOrderValidators(0, 0, 360);
        Assert.assertFalse(check_2.minSizeCheck.validate(newOrder(Side.SELL, 0)).isPresent());
        Assert.assertFalse(check_2.minSizeCheck.validate(newOrder(Side.SELL, 1)).isPresent());


        // deal with different min order size and trading unit
        NewOrderValidators check_3 = new NewOrderValidators(10, 1, 360);
        ErrorCode submit_3_1 = check_3.minSizeCheck.validate(newOrder(Side.SELL, 9)).get();
        Assert.assertEquals( ErrorCode.Type.REJECT, submit_3_1.type );
        Assert.assertEquals(RejectReason.MIN_SIZE, submit_3_1.rejectReason.get());
        Assert.assertFalse( check_3.minSizeCheck.validate(newOrder(Side.SELL, 10)).isPresent());
    }


    @Test
    public void testGtd() {
        int maxExpirationDayOffset = 360;
        NewOrderValidators check = new NewOrderValidators(1, 1, maxExpirationDayOffset);
        LocalDateTime now = LocalDateTime.now();

        Order gtdOrderValidToday = new Order().setTimeInForce(TimeInForce.GTD)
                .setExpirationDate(now.truncatedTo(ChronoUnit.DAYS));
        Assert.assertFalse( check.gtdCheck.validate(gtdOrderValidToday).isPresent());


        Order dayOrderValidToday = new Order().setTimeInForce(TimeInForce.DAY)
                .setExpirationDate(now.truncatedTo(ChronoUnit.DAYS));
        Assert.assertFalse( check.gtdCheck.validate(dayOrderValidToday).isPresent());


        Order dayOrderInvalidTomorrow = new Order().setTimeInForce(TimeInForce.DAY)
                .setExpirationDate(now.plusDays(1).truncatedTo(ChronoUnit.DAYS));
        assertFailedGtdValidation( check, dayOrderInvalidTomorrow);


        Order orderAtLastAcceptableDate = new Order().setTimeInForce(TimeInForce.GTD)
                .setExpirationDate(now.plusDays(maxExpirationDayOffset));
        Assert.assertFalse( check.gtdCheck.validate(orderAtLastAcceptableDate).isPresent() );


        Order orderInPast = new Order().setExpirationDate(now.minusDays(1));
        assertFailedGtdValidation(check, orderInPast);


        Order orderTooFarInFuture = new Order().setTimeInForce(TimeInForce.GTD)
                .setExpirationDate(now.plusDays(maxExpirationDayOffset + 1));
        assertFailedGtdValidation(check, orderTooFarInFuture);
    }


    private void assertFailedGtdValidation(NewOrderValidators check, Order order) {
        ErrorCode errorCode = check.gtdCheck.validate(order).get();
        Assert.assertEquals(ErrorCode.Type.REJECT, errorCode.type);
        Assert.assertEquals(RejectReason.GTD, errorCode.rejectReason.get());
    }

    private Order newOrder(Side side, int size ) {
        return new Order().setSide(side).setOrderQty(size);
    }
}
