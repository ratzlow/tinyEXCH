package net.tinyexch.ob.match.midpoint;

import net.tinyexch.ob.OrderbookSide;
import net.tinyexch.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Externalized qty collection loop offered as a stateless service.
 *
 * @author ratzlow@gmail.com
 * @since 2015-07-30
 */
class MatcherSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatcherSupport.class);
    public static final MatcherSupport SELF = new MatcherSupport();

    //------------------------------------------------------------------------------------------------------------------
    // public API
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Try to gather as much executable qty for the incoming midpoint order as possible.
     *
     * @param incoming incoming midpoint limit order
     * @param otherSide standing midpoint limit orders
     * @return the result of the match attempt
     */
    QtyCollector collectQty( Order incoming, OrderbookSide otherSide ) {
        Iterator<Order> iter = otherSide.getLimitOrders().iterator();
        QtyCollector qtyCollector = new QtyCollector();
        int incomingLeavesQty = incoming.getLeavesQty();
        while ( incomingLeavesQty > 0 && iter.hasNext() ) {
            Order otherSideOrder = iter.next();
            int executableQty = Math.min(incomingLeavesQty, otherSideOrder.getLeavesQty());

            if ( executableQty > otherSideOrder.getMinQty() ) {
                qtyCollector.add( new ExecutionChance(otherSideOrder, executableQty) );
                incomingLeavesQty -= executableQty;

            } else {
                // check if qty can be stolen from previous execution candidates
                int tryToStealQty = otherSideOrder.getMinQty() - executableQty;
                int stolenQty = qtyCollector.stealQty(tryToStealQty);
                if ( stolenQty > 0 ) {
                    qtyCollector.add( new ExecutionChance(otherSideOrder, executableQty + stolenQty));
                    // only reduce by net executed Qty
                    incomingLeavesQty -= ( executableQty - stolenQty );
                } else {
                    LOGGER.debug("No qty available to match {}", executableQty);
                }
            }
        }

        return qtyCollector;
    }
}
