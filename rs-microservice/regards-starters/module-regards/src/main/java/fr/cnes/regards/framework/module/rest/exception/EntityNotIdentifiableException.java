/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * Entity does not have any identifier
 *
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 */
public class EntityNotIdentifiableException extends EntityException {

    private static final long serialVersionUID = -1220166163207297225L;

    public EntityNotIdentifiableException(String pErrorMessage) {
        super(pErrorMessage);
    }
}
