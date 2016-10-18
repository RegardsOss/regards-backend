/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.jwt.exception;

/**
 * This exception is thrown when JWT signature is invalid so token cannot be trusted.
 *
 * @author msordi
 *
 */
public class InvalidJwtException extends JwtException {

    private static final long serialVersionUID = 1L;

    public InvalidJwtException(String pMessage) {
        super(pMessage);
    }
}
