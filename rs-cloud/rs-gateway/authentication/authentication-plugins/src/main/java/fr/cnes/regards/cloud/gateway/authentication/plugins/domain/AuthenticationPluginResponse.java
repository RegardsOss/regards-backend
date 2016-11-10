/* license_placeholder */
/*
 * VERSION-HISTORY
 *
 * VERSION : 1.0-SNAPSHOT : FR : FR-REGARDS-1 : 12/06/2015 : Creation
 *
 * END-VERSION-HISTORY
 */

package fr.cnes.regards.cloud.gateway.authentication.plugins.domain;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

/**
 *
 * Class AuthenticationPluginResponse
 *
 * Response class for authentication plugins.
 *
 * @author Sébastien Binda
 * @since 1.0
 */
public class AuthenticationPluginResponse {

    /**
     * Authentication status
     */
    @NotNull
    private AuthenticationStatus status;

    /**
     * User email
     */
    @NotNull
    @NotEmpty
    @Email
    private final String email;

    /**
     * Error message
     */
    private String errorMessage = null;

    public AuthenticationPluginResponse(final AuthenticationStatus pStatus, final String pEmail) {
        super();
        status = pStatus;
        email = pEmail;
    }

    public AuthenticationPluginResponse(final AuthenticationStatus pStatus, final String pEmail,
            final String pErrorMessage) {
        status = pStatus;
        email = pEmail;
        errorMessage = pErrorMessage;
    }

    /**
     * Get method.
     *
     * @return the status
     * @since 1.0
     */
    public AuthenticationStatus getStatus() {
        return status;
    }

    /**
     * Set method.
     *
     * @param pStatus
     *            the status to set
     * @since 1.0
     */
    public void setStatus(final AuthenticationStatus pStatus) {
        status = pStatus;
    }

    /**
     * Get method.
     *
     * @return the errorMessage
     * @since 1.0
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Set method.
     *
     * @param pErrorMessage
     *            the errorMessage to set
     * @since 1.0
     */
    public void setErrorMessage(final String pErrorMessage) {
        errorMessage = pErrorMessage;
    }

    public String getEmail() {
        return email;
    }

}
