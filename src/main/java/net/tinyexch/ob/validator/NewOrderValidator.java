package net.tinyexch.ob.validator;

import net.tinyexch.ob.ErrorCode;
import net.tinyexch.ob.Order;

import java.util.Optional;

/**
 * A specific check to be applied for new orders. These kind of checks might differ from replacements or cancels.
 *
 * @author ratzlow@gmail.com
 * @since 2014-07-29
 */
public interface NewOrderValidator {

    /**
     * @param order to be validated to the Orderbook
     * @return an error if there some violation was found otherwise empty.
     */
    Optional<ErrorCode> validate( Order order );
}
