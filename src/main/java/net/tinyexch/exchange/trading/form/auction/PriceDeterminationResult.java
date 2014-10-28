package net.tinyexch.exchange.trading.form.auction;

/**
 * Generated after the {@link net.tinyexch.exchange.trading.form.auction.PriceDeterminationPhase} of an auction run.
 *
 * @author ratzlow@gmail.com
 * @since 2014-10-06
 */
public class PriceDeterminationResult {
//    private double auctionPrice;
//    private int volume;

    private final double bidPrice;
    private final double askPrice;
    private final int matchableBidQty;
    private final int matchableAskQty;
    private final double auctionPrice;

    //----------------------------------------------------
    // constructors
    //----------------------------------------------------

    public PriceDeterminationResult(){
        this(0,0,0,0);
    }

    public PriceDeterminationResult(double bidPrice, double askPrice, int matchableBidQty, int matchableAskQty) {
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.matchableBidQty = matchableBidQty;
        this.matchableAskQty = matchableAskQty;
        this.auctionPrice = getBidSurplus() > getAskSurplus() ? bidPrice : askPrice;
    }


    //----------------------------------------------------
    // accessors
    //----------------------------------------------------

    public int getBidSurplus() { return matchableBidQty > matchableAskQty ? matchableBidQty - matchableAskQty : 0; }
    public int getAskSurplus() { return matchableBidQty < matchableAskQty ? matchableAskQty - matchableBidQty : 0; }

    public double getBidPrice() {
        return bidPrice;
    }

    public double getAskPrice() {
        return askPrice;
    }

    public int getMatchableBidQty() {
        return matchableBidQty;
    }

    public int getMatchableAskQty() {
        return matchableAskQty;
    }

    public double getAuctionPrice() {
        return auctionPrice;
    }

    //    public double getAuctionPrice() { return auctionPrice; }
//    public int getVolume() { return volume; }
}
