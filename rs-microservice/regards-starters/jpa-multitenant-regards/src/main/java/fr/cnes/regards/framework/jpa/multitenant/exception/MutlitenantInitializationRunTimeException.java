/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.exception;

/**
 *
 * Class MutlitenantInitializationRunTimeException
 *
 * Non-handled exception for JPA initialization failure
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class MutlitenantInitializationRunTimeException extends RuntimeException {

    /**
     * serialVersionUID field.
     */
    private static final long serialVersionUID = 3925220713661635450L;

    public MutlitenantInitializationRunTimeException(final String pMessage, final Throwable pCause) {
        super(pMessage, pCause);
    }

    public MutlitenantInitializationRunTimeException(final String pMessage) {
        super(pMessage);
    }

}
