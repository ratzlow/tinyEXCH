package net.tinyexch.exchange.trading.form.auction;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Generated after the {@link net.tinyexch.exchange.trading.form.auction.PriceDeterminationPhase} of an auction run.
 *
 * @author ratzlow@gmail.com
 * @since 2014-10-06
 */
public class PriceDeterminationResult {

    final static BiFunction<Integer, Integer, Integer> bidSurplusFunc =
            (matchableBidQty, matchableAskQty) -> matchableBidQty > matchableAskQty ? matchableBidQty - matchableAskQty : 0;
    final static BiFunction<Integer, Integer, Integer> askSurplusFunc =
            (matchableBidQty, matchableAskQty) -> matchableBidQty < matchableAskQty ? matchableAskQty - matchableBidQty : 0;

    private final Optional<Double> bidPrice;
    private final Optional<Double> askPrice;
    private final int matchableBidQty;
    private final int matchableAskQty;
    private final Optional<Double> auctionPrice;
    private final int bidSurplus;
    private final int askSurplus;

    //----------------------------------------------------
    // constructors
    //----------------------------------------------------

    public PriceDeterminationResult() {
        this(Optional.<Double>empty(), Optional.<Double>empty(), 0, 0, Optional.<Double>empty());
    }

    public PriceDeterminationResult(Optional<Double> bidPrice, Optional<Double> askPrice,
                                    int matchableBidQty, int matchableAskQty,
                                    Optional<Double> auctionPrice ) {
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.matchableBidQty = matchableBidQty;
        this.matchableAskQty = matchableAskQty;
        this.auctionPrice = auctionPrice;
        this.bidSurplus = bidSurplusFunc.apply(matchableBidQty, matchableAskQty);
        this.askSurplus = askSurplusFunc.apply(matchableBidQty, matchableAskQty);
    }


    //----------------------------------------------------
    // accessors
    //----------------------------------------------------

    public int getBidSurplus() { return bidSurplus; }
    public int getAskSurplus() { return askSurplus; }

    public Optional<Double> getBidPrice() {
        return bidPrice;
    }

    public Optional<Double> getAskPrice() {
        return askPrice;
    }

    public int getMatchableBidQty() {
        return matchableBidQty;
    }

    public int getMatchableAskQty() {
        return matchableAskQty;
    }

    public Optional<Double> getAuctionPrice() {
        return auctionPrice;
    }
}
