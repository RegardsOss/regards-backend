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
     * Identify root user
     */
    private final boolean isRoot;

    /**
     * Error message
     */
    private String errorMessage = null;

    public AuthenticationPluginResponse(final AuthenticationStatus pStatus, final String pEmail, String pErrorMessage,
            boolean pIsRoot) {
        super();
        this.status = pStatus;
        this.email = pEmail;
        this.errorMessage = pErrorMessage;
        this.isRoot = pIsRoot;
    }

    public AuthenticationPluginResponse(final AuthenticationStatus pStatus, final String pEmail) {
        this(pStatus, pEmail, null, false);
    }

    public AuthenticationPluginResponse(final AuthenticationStatus pStatus, final String pEmail,
            final String pErrorMessage) {
        this(pStatus, pEmail, pErrorMessage, false);
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

    public boolean isRoot() {
        return isRoot;
    }
}
