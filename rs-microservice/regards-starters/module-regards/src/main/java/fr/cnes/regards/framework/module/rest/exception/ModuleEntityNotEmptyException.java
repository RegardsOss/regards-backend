/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 *
 * Exception may occur when trying to delete a parent entity already linked to child entities.
 *
 * @author Marc Sordi
 *
 */
public class ModuleEntityNotEmptyException extends ModuleException {

    private static final long serialVersionUID = 1642202168751223657L;

    public ModuleEntityNotEmptyException(final Long pEntityIdentifier, final Class<?> pEntityClass) {
        super(String.format("Entity of type \"%s\" with id \"%s\" is not empty and cannot be removed.",
                            pEntityClass.getName(), pEntityIdentifier));
    }
}
