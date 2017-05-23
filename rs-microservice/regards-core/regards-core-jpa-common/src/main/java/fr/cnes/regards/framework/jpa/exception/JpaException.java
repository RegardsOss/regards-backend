/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.exception;

/**
 *
 * Generic JPA exception
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class JpaException extends Exception {

    public JpaException(final String pMessage, final Throwable pCause) {
        super(pMessage, pCause);
    }

    public JpaException(final String pMessage) {
        super(pMessage);
    }

    public JpaException(final Throwable pCause) {
        super(pCause);
    }

}
