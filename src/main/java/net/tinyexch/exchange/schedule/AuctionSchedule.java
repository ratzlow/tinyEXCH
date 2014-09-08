package net.tinyexch.exchange.schedule;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static net.tinyexch.exchange.trading.form.auction.AuctionState.CALL_STOPPED;
import static net.tinyexch.exchange.trading.form.auction.AuctionState.PRICE_DETERMINATION_STOPPED;

/**
 * This class generates the needed trading phase triggers for an auction.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-30
 */
public class AuctionSchedule implements TradingFormSchedule {

    private final Random random = new Random(LocalTime.now().getNano());

    /** If the type is not specified the trading form is already preconfigured */
//    private final TradingFormRunType type;

    /** default generator */
//    private Supplier<Auction> generator;

    private LocalTime callStartTime;
    private Duration minCallDuration;
    private Duration randomCallDuration;

    private boolean orderbookBalancingRequired = true;
    private final TradingFormInitializer initializer;

    //
    // constructors
    //

    private AuctionSchedule(TradingFormInitializer initializer) {
        this.initializer = initializer;
    }

    public static AuctionSchedule kickOff(TradingFormInitializer initializer) {
        return new AuctionSchedule( initializer );
    }

    //
    // builder API
    //

    public AuctionSchedule startingCallPhaseAt(LocalTime callStartTime) {
        this.callStartTime = callStartTime;
        return this;
    }

    public AuctionSchedule andLastForAtLeast( Duration minCallDuration ) {
        this.minCallDuration = minCallDuration;
        return this;
    }

    public AuctionSchedule withRandomBufferBetween( Duration randomCallDuration ) {
        this.randomCallDuration = randomCallDuration;
        return this;
    }

    public AuctionSchedule includeOrderbookBalancing(boolean include) {
        orderbookBalancingRequired = include;
        return this;
    }

    @Override
    public List<TradingPhaseTrigger> getTriggers() {
        List<TradingPhaseTrigger> triggers = new ArrayList<>();

        // start call phase at fixed time
        triggers.add( new TradingPhaseTrigger(StateChangerFactory.startCall(
                ),
                callStartTime)
        );
        // call ends after minimum time extend by a random period within a given range
        LocalTime callStopTime = callStartTime.plus(minCallDuration).plus(getSubDuration(randomCallDuration));
        triggers.add( new TradingPhaseTrigger(StateChangerFactory.stopCall(), callStopTime) );

        // start price determination phase
        triggers.add( new TradingPhaseTrigger(StateChangerFactory.determinePrice(), CALL_STOPPED) );

        // not all auction types need a balancing phase
        if ( orderbookBalancingRequired ) {
            triggers.add( new TradingPhaseTrigger(StateChangerFactory.balanceOrderbook(), PRICE_DETERMINATION_STOPPED) );
        }

        return triggers;
    }

    public TradingFormInitializer getInitializer() { return initializer; }

    private Duration getSubDuration( Duration max ) {
        long millis = max.getNano() / 1_000_000;
        if ( millis > Integer.MAX_VALUE) {
            throw new SchedulerException( "Random max duration too large! ->" + max);
        }
        int valueInRange = random.nextInt((int) millis);
        return Duration.ofMillis(valueInRange);
    }

}
