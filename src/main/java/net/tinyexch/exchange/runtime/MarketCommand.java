package net.tinyexch.exchange.runtime;

import net.tinyexch.exchange.trading.model.ContinuousTradingInterruptedByAuctions;

/**
 * // TODO (FRa) : (FRa) : comment
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-22
 */
public abstract class MarketCommand {

    public abstract void execute( ContinuousTradingInterruptedByAuctions market );
}
