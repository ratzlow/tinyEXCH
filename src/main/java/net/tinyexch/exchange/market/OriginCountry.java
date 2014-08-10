package net.tinyexch.exchange.market;

/**
 * Where is the order submitter coming from.
 *
 * // TODO (FRa) : (FRa) : FIX tag?
 * @author ratzlow@gmail.com
 * @since 2014-08-02
 */
public enum OriginCountry {
    /**
     * Same country that runs the exchange
     */
    DOMESTIC,

    /**
     * Outside of the exchange country
     */
    FOREIGN
}
