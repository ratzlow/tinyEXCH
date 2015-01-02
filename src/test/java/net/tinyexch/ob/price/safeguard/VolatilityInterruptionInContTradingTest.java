package net.tinyexch.ob.price.safeguard;

import net.tinyexch.exchange.event.DefaultNotificationListener;
import net.tinyexch.exchange.event.produce.VolatilityInterruptionEventHandler;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading;
import net.tinyexch.ob.SubmitType;
import net.tinyexch.ob.match.Match;
import net.tinyexch.ob.match.MatchEngine;
import net.tinyexch.order.Order;
import net.tinyexch.order.OrderType;
import net.tinyexch.order.Side;
import net.tinyexch.order.Trade;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test life cycle of volatility interruptions in continuous trading.
 *
 * @author ratzlow@gmail.com
 * @since 2014-10-05
 */
// TODO (FRa) : (FRa) : test update of dyn ref price
public class VolatilityInterruptionInContTradingTest {

    @Test
    public void testInterruptionRaisedInContTrading() {
        Order buy = Order.of(Integer.valueOf(1).toString(), Side.BUY).setOrderType(OrderType.LIMIT);
        Order sell = Order.of(Integer.valueOf(2).toString(), Side.BUY).setOrderType(OrderType.LIMIT);

        Trade trade = new Trade(buy, sell, 14.6, 20, 0);
        MatchEngine matchEngine = ( order, otherSide) -> new Match( order, Collections.singletonList(trade), Match.State.ACCEPT );

        List<VolatilityInterruption> raisedInterruptions = new ArrayList<>();
        VolatilityInterruptionEventHandler interruptionEventHandler = new VolatilityInterruptionEventHandler() {
            @Override
            public void handle(VolatilityInterruption event) {
                super.handle(event);
                raisedInterruptions.add( event );
            }
        };

        DefaultNotificationListener notificationListener = new DefaultNotificationListener();
        notificationListener.setVolatilityInterruptionEventHandler(interruptionEventHandler);

        VolatilityInterruptionGuard interruptionGuard = new VolatilityInterruptionGuard( 10, 20, 12, 10 );
        ContinuousTrading contTrading = new ContinuousTrading( notificationListener, matchEngine, interruptionGuard );

        contTrading.submit( buy, SubmitType.NEW );
        Assert.assertEquals(1, raisedInterruptions.size());
        Assert.assertEquals( true, contTrading.isVolatilityInterrupted() );
    }
}
