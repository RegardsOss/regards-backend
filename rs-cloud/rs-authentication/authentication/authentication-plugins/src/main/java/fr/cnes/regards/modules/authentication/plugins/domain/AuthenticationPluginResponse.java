/* license_placeholder */
/*
 * VERSION-HISTORY
 *
 * VERSION : 1.0-SNAPSHOT : FR : FR-REGARDS-1 : 12/06/2015 : Creation
 *
 * END-VERSION-HISTORY
 */

package fr.cnes.regards.modules.authentication.plugins.domain;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Class AuthenticationPluginResponse
 * <p>
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
    private Boolean accessGranted = Boolean.FALSE;

    /**
     * User email
     */
    @NotNull
    @NotEmpty
    @Email
    private final String email;

    /**
     * PluginClassName from the plugin who created this response.
     * This parameter is automaticly set by the authentication manager.
     * Each plugin don't need to set this value.
     */
    private String pluginClassName = "";

    /**
     * Error message
     */
    private String errorMessage = null;

    public AuthenticationPluginResponse(final Boolean pAccessGranted, final String pEmail, final String pErrorMessage) {
        super();
        this.accessGranted = pAccessGranted;
        this.email = pEmail;
        this.errorMessage = pErrorMessage;
    }

    public AuthenticationPluginResponse(final Boolean pAccessGranted, final String pEmail) {
        this(pAccessGranted, pEmail, null);
    }

    public Boolean getAccessGranted() {
        return accessGranted;
    }

    public void setAccessGranted(final Boolean pAccessGranted) {
        accessGranted = pAccessGranted;
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
     * @param pErrorMessage the errorMessage to set
     * @since 1.0
     */
    public void setErrorMessage(final String pErrorMessage) {
        errorMessage = pErrorMessage;
    }

    public String getEmail() {
        return email;
    }

    public String getPluginClassName() {
        return pluginClassName;
    }

    public void setPluginClassName(final String pPluginClassName) {
        pluginClassName = pPluginClassName;
    }

}
