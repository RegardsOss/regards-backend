package fr.cnes.regards.modules.order.domain.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * @author oroussel
 */
public class EmptyBasketException extends ModuleException {
    public EmptyBasketException() {
        super("Basket is empty");
    }

    public EmptyBasketException(String msg) {
        super(msg);
    }
}
