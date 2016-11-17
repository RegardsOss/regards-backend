/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 *
 * Error occurs when entity has to be created but already has an identifier!
 *
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 */
public class EntityUnexpectedIdentifierException extends EntityException {

    /**
    *
    */
    private static final long serialVersionUID = 5122349845042630472L;

    public EntityUnexpectedIdentifierException(final Long pEntityIdentifier, final Class<?> pEntityClass) {
        super(String.format("Cannot create entity of type \"%s\" that already has an identifier \"%s\".",
                            pEntityClass.getName(), pEntityIdentifier));
    }

}
