/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.exception;

/**
 *
 * Class MultiDataBasesException
 *
 * Exception raised when there is an error in the multitenancy databases access
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class MultiDataBasesException extends Exception {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = 7382111289929689769L;

    /**
     *
     * Constructor
     *
     * @param pMessage
     *            message
     * @param pCause
     *            cause
     * @since 1.0-SNAPSHOT
     */
    public MultiDataBasesException(final String pMessage, final Throwable pCause) {
        super(pMessage, pCause);
    }

    /**
     *
     * Constructor
     *
     * @param pMessage
     *            message
     * @since 1.0-SNAPSHOT
     */
    public MultiDataBasesException(final String pMessage) {
        super(pMessage);
    }

    /**
     *
     * Constructor
     *
     * @param pCause
     *            cause
     * @since 1.0-SNAPSHOT
     */
    public MultiDataBasesException(final Throwable pCause) {
        super(pCause);
    }

}
