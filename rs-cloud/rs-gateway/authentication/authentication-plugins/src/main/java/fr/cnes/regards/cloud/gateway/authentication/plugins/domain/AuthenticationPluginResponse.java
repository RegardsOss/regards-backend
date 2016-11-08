/* license_placeholder */
/*
 * VERSION-HISTORY
 *
 * VERSION : 1.0-SNAPSHOT : FR : FR-REGARDS-1 : 12/06/2015 : Creation
 *
 * END-VERSION-HISTORY
 */

package fr.cnes.regards.cloud.gateway.authentication.plugins.domain;

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
    private AuthenticationStatus status;

    /**
     * User role
     */
    private String role;

    /**
     * Error message
     */
    private String errorMessage = null;

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

    /**
     * Get method.
     *
     * @return the role
     * @since 1.0-SNAPSHOT
     */
    public String getRole() {
        return role;
    }

    /**
     * Set method.
     *
     * @param pRole
     *            the role to set
     * @since 1.0-SNAPSHOT
     */
    public void setRole(final String pRole) {
        role = pRole;
    }

}
