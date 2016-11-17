/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 * Exception to indicate that the operation is forbidden on entity.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sylvain Vissiere-Guerinet
 * @since 1.0-SNAPSHOT
 */
public class EntityOperationForbiddenException extends EntityException {

    /**
     * Serial
     */
    private static final long serialVersionUID = 1056576133397279032L;

    /**
     * Creates a new {@link OperationForbiddenException} with passed params.
     *
     * @param pEntityIdentifier
     *            Entity identifier
     * @param pEntityClass
     *            Entity class
     * @param pMessage
     *            Message describing the forbidden operation
     * @since 1.0-SNAPSHOT
     */
    public EntityOperationForbiddenException(final String pEntityIdentifier, final Class<?> pEntityClass,
            final String pMessage) {
        super(String.format("Operation on entity %s with id: %s is forbidden: %s", pEntityClass.getName(),
                            pEntityIdentifier, pMessage));
    }

}
