/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins;

import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationPluginResponse;
import fr.cnes.regards.modules.plugins.annotations.PluginInterface;

/**
 *
 * Class IAuthenticationProvider
 *
 * Authentication Provider interface.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@PluginInterface
@FunctionalInterface
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
