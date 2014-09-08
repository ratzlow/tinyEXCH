package net.tinyexch.exchange.trading.form.continuous;


/**
 * Operation mode of the current continuous trading.
 *
 * @author ratzlow@gmail.com
 * @since 2014-08-03
 */
public enum ContinuousTradingState {
    RUNNING(true),
    STOPPED(false),

    INACTIVE(false);

    private final boolean active;

    ContinuousTradingState(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
