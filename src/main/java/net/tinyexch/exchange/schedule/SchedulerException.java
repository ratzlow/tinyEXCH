package net.tinyexch.exchange.schedule;

/**
 * Raised if an invalid schedule plan for a trading day is specified.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-31
 */
public class SchedulerException extends RuntimeException {

    public SchedulerException(String message) { super(message); }
}
