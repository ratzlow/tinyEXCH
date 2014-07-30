package net.tinyexch.usermgmt;

/**
 * A corporation admitted to trade on the exchange.
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-27
 * @link chap 4
 */
public class TradingMember {
    public enum Type {
        AGENT_TRADER,
        PROP_TRADER,

        /**
         * "Designated Sponsor"
         */
        LIQUIDITY_PROVIDER,

        LIQUIDITY_MANAGER,
    }

    private Type memberType;
}
