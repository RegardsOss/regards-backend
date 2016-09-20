/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.security.jwt.exception;

/**
 * This exception is thrown when JWT signature is invalid so token cannot be trusted.
 *
 * @author msordi
 *
 */
public class MissingClaimException extends Exception {

    private static final long serialVersionUID = 1L;

    public MissingClaimException(String pClaimKey) {
        super("Missing " + pClaimKey);
    }
}
