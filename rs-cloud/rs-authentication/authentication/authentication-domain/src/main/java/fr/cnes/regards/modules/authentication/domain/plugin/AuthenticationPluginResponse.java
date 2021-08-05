/* license_placeholder */
/*
 * VERSION-HISTORY
 *
 * VERSION : 1.0-SNAPSHOT : FR : FR-REGARDS-1 : 12/06/2015 : Creation
 *
 * END-VERSION-HISTORY
 */

package fr.cnes.regards.modules.authentication.domain.plugin;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

/**
 * Class AuthenticationPluginResponse
 *
 * Response class for authentication plugins.
 * @author Sébastien Binda
 * @since 1.0
 */
public class AuthenticationPluginResponse {

    private boolean accessGranted;

    @NotEmpty
    @Email
    private final String email;

    /**
     * PluginClassName from the plugin who created this response.
     * This parameter is automatically set by the authentication manager.
     * Each plugin don't need to set this value.
     */
    private String pluginClassName = "";

    private String errorMessage;

    private String serviceProviderName;

    public AuthenticationPluginResponse(boolean accessGranted, String email, String errorMessage, String serviceProviderName) {
        this.accessGranted = accessGranted;
        this.email = email;
        this.errorMessage = errorMessage;
        this.serviceProviderName = serviceProviderName;
    }

    public AuthenticationPluginResponse(boolean pAccessGranted, String pEmail, String pErrorMessage) {
        this.accessGranted = pAccessGranted;
        this.email = pEmail;
        this.errorMessage = pErrorMessage;
    }

    public AuthenticationPluginResponse(boolean pAccessGranted, String pEmail) {
        this(pAccessGranted, pEmail, null);
    }

    public boolean isAccessGranted() {
        return accessGranted;
    }

    public AuthenticationPluginResponse setAccessGranted(boolean accessGranted) {
        this.accessGranted = accessGranted;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public String getPluginClassName() {
        return pluginClassName;
    }

    public AuthenticationPluginResponse setPluginClassName(String pluginClassName) {
        this.pluginClassName = pluginClassName;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public AuthenticationPluginResponse setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public String getServiceProviderName() {
        return serviceProviderName;
    }

    public AuthenticationPluginResponse setServiceProviderName(String serviceProviderName) {
        this.serviceProviderName = serviceProviderName;
        return this;
    }

}
