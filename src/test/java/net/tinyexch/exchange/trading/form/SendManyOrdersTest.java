package net.tinyexch.exchange.trading.form;

import net.tinyexch.exchange.event.NotificationListener;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTrading;
import net.tinyexch.exchange.trading.form.continuous.ContinuousTradingState;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * // TODO (FRa) : (FRa) : comment
 *
 * @author ratzlow@gmail.com
 * @since 2015-05-21
 */
public class SendManyOrdersTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendManyOrdersTest.class);
    private int sequence = 0;

    @Test
    public void sendRandomOrdersForContinuousTrading() {
        ContinuousTrading trading = new ContinuousTrading(NotificationListener.NO_OP);
        trading.start();
        trading.getOrderbook().open();
        trading.transitionTo(ContinuousTradingState.RUNNING);
/*
        Stream.<Order>generate(this::generate)
                .peek(o -> LOGGER.info(o.getClientOrderID()))
                .forEach(order -> trading.submit(order, SubmitType.NEW));
                */
    }

/*
    private Order generate( int discriminator ) {
        final Order order;
        sw

        if (count % 2 == 0 ) {
            order = OrderFactory.buyL()

        } else if ( count % 3 == 0 ) {
            order = OrderFactory.sellL()

        }
    }
*/

}
