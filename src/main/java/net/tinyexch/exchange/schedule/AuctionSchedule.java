package net.tinyexch.exchange.schedule;

import net.tinyexch.exchange.trading.form.auction.AuctionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Setup the schedule for a particular auction.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-07
 */
public class AuctionSchedule {
    private final AuctionType auctionType;
    private final List<TradingPhaseTrigger> triggers = new ArrayList<>();

    public AuctionSchedule(AuctionType auctionType) {
        this.auctionType = auctionType;
    }

    public List<TradingPhaseTrigger> getTriggers() {
        return triggers;
    }
}
