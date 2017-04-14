/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.exception;

import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

import fr.cnes.regards.framework.authentication.internal.AuthenticationStatus;

/**
 *
 * Class AuthenticationException
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class AuthenticationException extends OAuth2Exception {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = 1L;

    /**
     * Error additional key into RequestEntity response
     */
    private static final String ERROR_TYPE_KEY = "error";

    public AuthenticationException(final String pMsg, final AuthenticationStatus pStatus) {
        super(pMsg);
        this.addAdditionalInformation(ERROR_TYPE_KEY, pStatus.toString());
    }

}
