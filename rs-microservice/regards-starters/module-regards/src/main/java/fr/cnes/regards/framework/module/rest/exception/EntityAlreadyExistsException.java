/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 *
 * Element in conflict that already exists
 *
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class EntityAlreadyExistsException extends EntityException {

    private static final long serialVersionUID = 10460690591381017L;

    public EntityAlreadyExistsException(String pErrorMessage) {
        super(pErrorMessage);
    }

}
