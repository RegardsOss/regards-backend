package fr.cnes.regards.modules.order.domain.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * @author oroussel
 */
public class EmptySelectionException extends ModuleException {
    public EmptySelectionException() {
        super("This selection contains no file that can be ordered");
    }
}
