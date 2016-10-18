/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.jwt.exception;

/**
 * @author svissier
 *
 */
public class JwtException extends Exception {

    private static final long serialVersionUID = 1L;

    public JwtException(String pClaimKey) {
        super("Missing " + pClaimKey);
    }
}
