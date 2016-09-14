/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.configuration.security;

/**
 *
 * Class WebSecurityException
 *
 * Security Execption
 *
 * @author CS
 * @since 0.0.1
 */
public class WebSecurityException extends Exception {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 0.0.1
     */
    private static final long serialVersionUID = 1L;

    public WebSecurityException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }

    public WebSecurityException(Throwable pCause) {
        super(pCause);
    }

}
