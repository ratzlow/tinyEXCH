package net.tinyexch.ob.match.midpoint;

import net.tinyexch.order.Order;

/**
 * Snapshot of potential execution for a midpoint order. Starts with a max quantity as specified in constructor but
 * might be reduced over time via {@link #stealQty}.
 *
 * This means the executable quantity ranges from:
 * - max: as specified in constructor
 * - min: otherSide.minQty
 *
 * This class im mutable and thus not thread safe!!
 *
 * @author ratzlow@gmail.com
 * @since 2015-07-30
 */
class ExecutionChance {
    final Order otherSide;
    int executableQty;

    /**
     * @param otherSide against the incoming should be executed
     * @param executableQty max qty that might be executed against #otherSide
     */
    ExecutionChance(Order otherSide, int executableQty ) {
        this.otherSide = otherSide;
        this.executableQty = executableQty;
    }

    /**
     * Try to take some of the executable qty from previous attempts to distribute the gained qty against other orders.
     *
     * @param stealDemandQty qty we want to have to balance against other other side orders
     * @return qty that could be reclaimed respecting the otherSideOrder.minQty limit
     */
    int stealQty( int stealDemandQty ) {
        int stealableQty = executableQty - otherSide.getMinQty();
        int availableStealQty = Math.min( stealableQty, stealDemandQty );
        executableQty -= availableStealQty;
        return availableStealQty;
    }

    /**
     * @return the one provided in constructor
     */
    Order getOtherSide() { return otherSide; }

    /**
     * @return the executable qty at current time
     */
    int getExecutableQty() { return executableQty; }
}

