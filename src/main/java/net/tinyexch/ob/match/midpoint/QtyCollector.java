package net.tinyexch.ob.match.midpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Balancer to optimize executable volume for midpoint orders. You can add no execution attempts and if required steal
 * volume from previous execution attempts.
 *
 * Since it is mutable it is not thread safe!!
 *
 * @author ratzlow@gmail.com
 * @since 2015-07-30
 */
class QtyCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(QtyCollector.class);

    private final Deque<ExecutionChance> potentialMatches = new ArrayDeque<>();

    //-------------------------------------------------------------------------------------
    // public API
    //-------------------------------------------------------------------------------------

    /**
     * By going through the book top -> down we identify order which could server as an execution candidate. This
     * effective quantity being executed might change during the matching process.
     *
     * @param chance of execution against other side
     */
    void add( ExecutionChance chance ) {
        potentialMatches.push( chance );
    }

    /**
     * In order to optimize the overall executable qty it might be needed to reclaim already executed qty from previous
     * matches.
     *
     * @param gapToCloseRequiredMin min qty to allow another order to be executed
     * @return the stolenQty from previous execution chances or 0 if nothing could be taken
     */
    int stealQty(final int gapToCloseRequiredMin) {
        if ( potentialMatches.isEmpty() ) return 0;

        int totalStolenQty = 0;
        Iterator<ExecutionChance> iter = potentialMatches.iterator();
        while ( iter.hasNext() && totalStolenQty < gapToCloseRequiredMin ) {
            ExecutionChance exec = iter.next();
            if (gapToCloseRequiredMin - totalStolenQty > 0) {
                int stealDemandQty = gapToCloseRequiredMin - totalStolenQty;
                totalStolenQty += exec.stealQty( stealDemandQty );
            } else {
                LOGGER.info("Continue collecting even though not needed!");
            }
        }

        return totalStolenQty;
    }

    /**
     * @return stack of previous execution attempts. At the bottom of the stack are the execution attempts with highest
     * precedence so taken qty off them is less favorable.
     */
    Deque<ExecutionChance> getPotentialMatches() {
        return potentialMatches;
    }
}