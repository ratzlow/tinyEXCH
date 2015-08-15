package net.tinyexch.ob.match;

import net.tinyexch.ob.price.safeguard.VolatilityInterruption;
import net.tinyexch.order.Trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Container for all executions created during the match process.
 *
 * @author ratzlow@gmail.com
 * @since 2015-07-23
 */
public class MatchCollector {
    private final List<Trade> trades = new ArrayList<>();
    private Optional<VolatilityInterruption> volatilityInterruption = Optional.empty();


    public List<Trade> getTrades() { return trades; }

    public Optional<VolatilityInterruption> getVolatilityInterruption() { return volatilityInterruption; }

    public void setVolatilityInterruption(Optional<VolatilityInterruption> volatilityInterruption) {
        this.volatilityInterruption = volatilityInterruption;
    }
}
