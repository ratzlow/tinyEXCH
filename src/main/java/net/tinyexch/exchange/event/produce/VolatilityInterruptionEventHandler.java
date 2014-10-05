package net.tinyexch.exchange.event.produce;

import net.tinyexch.ob.price.safeguard.VolatilityInterruption;

/**
 * Responsible to kick off a switch of trading form if the event occurs in continuous trading.
 *
 * @author ratzlow@gmail.com
 * @since 2014-10-05
 * @link chap 11
 */
public class VolatilityInterruptionEventHandler {

    public void handle(VolatilityInterruption event) {
        // TODO (FRa) : (FRa) : switch trading form if such an event is raised
    }
}
