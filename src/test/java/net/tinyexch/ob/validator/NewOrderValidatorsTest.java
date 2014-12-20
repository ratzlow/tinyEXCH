package net.tinyexch.ob.validator;

import net.tinyexch.ob.ErrorCode;
import net.tinyexch.ob.RejectReason;
import net.tinyexch.order.*;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

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
    public void testStrikeMatchOrder() {
        NewOrderValidators check = new NewOrderValidators();
        NewOrderValidator rule = check.strikeMatchOrderTypeCheck;

        assertEquals( Optional.<ErrorCode>empty(), rule.validate(createValidSMO()) );

        // invalid price
        Order o_1 = createValidSMO().setStopPrice(0);

        // SMO is only valid today
        Order o_2 = createValidSMO().setTimeInForce(TimeInForce.GTD).setExpirationDate(LocalDateTime.now().plusDays(1));

        // wrong trading phase
        Order o_3 = createValidSMO().setTradingSessionSubID(TradingSessionSubID.Continuous);

        // simple stop order -> SMO is exactly the opposite
        Order o_4 = createValidSMO().setDiscretionLimitType(DiscretionLimitType.OR_BETTER);

        Stream.of(o_1, o_2, o_3, o_4)
                .forEach(o -> assertEquals("Expected to fail " + o, ErrorCode.Type.REJECT, rule.validate(o).get().type));
    }

    private Order createValidSMO() {
        return Order.of(UUID.randomUUID().toString(), Side.BUY)
                .setOrderType(OrderType.STRIKE_MATCH)
                .setTradingSessionSubID(TradingSessionSubID.ClosingOrClosingAuction)
                .setStopPrice(5).setTimeInForce(TimeInForce.DAY).setDiscretionLimitType(DiscretionLimitType.OR_WORSE);
    }

    @Test
    public void testMinSizeCheck() {
        NewOrderValidators check_1 = new NewOrderValidators(1, 1, 360, acceptedOrderTypes);
        assertEquals(
                ErrorCode.Type.REJECT,
                check_1.minSizeCheck.validate(newOrder(Side.SELL, 0)).get().type);
        Assert.assertFalse(
                check_1.minSizeCheck.validate(newOrder(Side.SELL, 1)).isPresent());

        // no min sizes set, so everything can be submitted
        NewOrderValidators check_2 = new NewOrderValidators(0, 0, 360, acceptedOrderTypes);
        Assert.assertFalse(check_2.minSizeCheck.validate(newOrder(Side.SELL, 0)).isPresent());
        Assert.assertFalse(check_2.minSizeCheck.validate(newOrder(Side.SELL, 1)).isPresent());


        // deal with different min order size and trading unit
        NewOrderValidators check_3 = new NewOrderValidators(10, 1, 360, acceptedOrderTypes);
        ErrorCode submit_3_1 = check_3.minSizeCheck.validate(newOrder(Side.SELL, 9)).get();
        assertEquals(ErrorCode.Type.REJECT, submit_3_1.type);
        assertEquals(RejectReason.MIN_SIZE, submit_3_1.rejectReason.get());
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
        assertEquals(ErrorCode.Type.REJECT, errorCode.type);
        assertEquals(RejectReason.GTD, errorCode.rejectReason.get());
    }

    private Order newOrder(Side side, int size ) {
        return new Order(newOrderID()).setSide(side).setOrderQty(size);
    }

    private String newOrderID() {
        return Integer.valueOf(++sequence).toString();
    }
}
