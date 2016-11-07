/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.rest.exception;

/**
 *
 * Class EntityException
 *
 * Global entities exceptions
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class EntityException extends Exception {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = -6284091634593228162L;

    /**
     *
     * Constructor
     *
     * @param pMessage
     *            Entity error message
     * @since 1.0-SNAPSHOT
     */
    public EntityException(final String pMessage) {
        super(pMessage);
    }

}
