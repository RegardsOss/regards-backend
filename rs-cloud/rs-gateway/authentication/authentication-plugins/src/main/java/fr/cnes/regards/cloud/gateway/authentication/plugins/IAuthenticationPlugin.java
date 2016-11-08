/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins;

import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationPluginResponse;

/**
 *
 * Class IAuthenticationProvider
 *
 * Authentication Provider interface. TODO : Should be replace with the REGARDS Plugins system.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface IAuthenticationPlugin {

    /**
     *
     * Check if the couple pName/pPassowrd is valid for the given project pScope
     *
     * @param pName
     *            user login
     * @param pPassword
     *            user password
     * @param pScope
     *            user project
     * @return Authentication status UserStatus
     * @since 1.0-SNAPSHOT
     */
    AuthenticationPluginResponse authenticate(String pName, String pPassword, String pScope);

}
