/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.exception;

/**
 *
 * Class EntityNotFoundException
 *
 * Exception to indicates that the required entity is not found.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class EntityNotFoundException extends Exception {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = -7255117056559968468L;

    /**
     *
     * Constructor
     *
     * @param pEntityIdentifier
     *            Entity identifier
     * @param pEntityClass
     *            Entity class
     * @since 1.0-SNAPSHOT
     */
    public EntityNotFoundException(final String pEntityIdentifier, final Class<?> pEntityClass) {
        super(String.format("Entity %s with id : %s doesn't exists", pEntityClass.getName(), pEntityIdentifier));
    }
}
