/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 *
 * Exception may occur when trying to delete a parent entity already linked to child entities.
 *
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class EntityNotEmptyException extends EntityException {

    private static final long serialVersionUID = 1642202168751223657L;

    public EntityNotEmptyException(final Long pEntityIdentifier, final Class<?> pEntityClass) {
        super(String.format("Entity of type \"%s\" with id \"%s\" is not empty and cannot be removed.",
                            pEntityClass.getName(), pEntityIdentifier));
    }
}
