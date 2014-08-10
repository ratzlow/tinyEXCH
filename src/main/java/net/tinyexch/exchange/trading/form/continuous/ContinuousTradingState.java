package net.tinyexch.exchange.trading.form.continuous;

/**
 * Operation mode of the current continuous trading.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-03
 */
public enum ContinuousTradingState {
    RUNNING(true),
    STOPPED(false);

    private final boolean active;

    ContinuousTradingState(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * @return get the first active state of an auction lifecycle
     */
    public static ContinuousTradingState start() { return values()[0]; }

    /**
     * @return get the last state after the auction is done and closed
     */
    public static ContinuousTradingState close() { return values()[values().length - 1]; }
}
