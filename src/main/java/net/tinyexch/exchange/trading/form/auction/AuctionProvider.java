package net.tinyexch.exchange.trading.form.auction;

/**
 * Trading models supporting auctions should implement this interface to enable auctions to kick in.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-06
 */
public interface AuctionProvider {
    Auction getAuction();
}
