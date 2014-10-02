package net.tinyexch.exchange.event.consume;

import net.tinyexch.exchange.trading.model.TradingFormRunType;

import java.util.Optional;

/**
 * Raised if a {@link net.tinyexch.exchange.trading.form.TradingForm} switches through it's life cycle states.
 *
 * @author ratzlow@gmail.com
 * @since 2014-09-27
 */
public class ChangeStateEvent {

    private final Enum targetState;
    private final Optional<TradingFormRunType> tradingFormRunType;

    public ChangeStateEvent(Enum targetState) {
        this(targetState, null);
    }

    public ChangeStateEvent(Enum targetState, TradingFormRunType runType) {
        this.targetState = targetState;
        this.tradingFormRunType = Optional.ofNullable(runType);
    }



    public Enum getTargetState() {
        return targetState;
    }

    public Optional<TradingFormRunType> getTradingFormRunType() {
        return tradingFormRunType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ChangeStateEvent{");
        sb.append(", targetState=").append(targetState);
        sb.append(", tradingFormRunType=").append(tradingFormRunType);
        sb.append('}');
        return sb.toString();
    }
}
