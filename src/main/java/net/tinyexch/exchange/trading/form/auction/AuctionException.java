package net.tinyexch.exchange.trading.form.auction;

/**
 * Indicates an error condition occurred during an auction.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-09
 */
public class AuctionException extends RuntimeException {

    public AuctionException(String message) {
        super(message);
    }

    public AuctionException(String message, Throwable cause) {
        super(message, cause);
    }
}
