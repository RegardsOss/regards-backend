/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * Element in conflict that already exists
 *
 * @author Marc Sordi
 *
 */
public class AlreadyExistsException extends ModuleException {

    private static final long serialVersionUID = 10460690591381017L;

    public AlreadyExistsException(String pErrorMessage) {
        super(pErrorMessage);
    }

}
