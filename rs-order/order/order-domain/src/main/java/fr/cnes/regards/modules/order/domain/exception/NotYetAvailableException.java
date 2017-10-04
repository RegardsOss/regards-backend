package fr.cnes.regards.modules.order.domain.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * @author oroussel
 */
public class NotYetAvailableException extends ModuleException {

    public NotYetAvailableException() {
        super("Not yet available.");
    }
}
