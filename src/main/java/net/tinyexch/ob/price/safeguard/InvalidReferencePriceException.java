package net.tinyexch.ob.price.safeguard;

/**
 * Thrown if the submitted reference prices do not make up valid intersecting price range.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-12
 */
public class InvalidReferencePriceException extends RuntimeException {
    public InvalidReferencePriceException(String message) {
        super(message);
    }
}
