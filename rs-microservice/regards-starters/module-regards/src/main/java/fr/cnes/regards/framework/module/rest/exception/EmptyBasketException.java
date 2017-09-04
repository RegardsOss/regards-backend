package fr.cnes.regards.framework.module.rest.exception;

/**
 * @author oroussel
 */
public class EmptyBasketException extends ModuleException {
    public EmptyBasketException() {
        super("Basket is empty");
    }
}
