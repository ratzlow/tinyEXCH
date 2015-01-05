package net.tinyexch.ob.match;

/**
 * Thrown if an unmatchable order is passed to the match engine. This happens if the order has properties that prevents
 * it from execution.
 *
 * @author ratzlow@gmail.com
 * @since 2015-01-05
 */
public class MatchException extends RuntimeException {

    public MatchException(String message) {
        super(message);
    }
}
