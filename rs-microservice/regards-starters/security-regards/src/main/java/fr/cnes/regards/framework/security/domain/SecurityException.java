/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.domain;

/**
 * This exception is thrown when an error occurs during security check process
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 *
 */
public class SecurityException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -3031544861738056012L;

    /**
     * Constructor
     *
     * @param pMessage
     *            the message
     * @param pCause
     *            the cause
     */
    public SecurityException(final String pMessage, final Throwable pCause) {
        super(pMessage, pCause);
    }

    public SecurityException(Throwable cause) {
        super(cause);
    }
}
