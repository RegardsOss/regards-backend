/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins;

import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.modules.plugins.annotations.PluginInterface;

/**
 *
 * Class IServiceProviderPlugin
 *
 * Interface for all Service Provider plugins.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@PluginInterface(description="Interface for all Service Provider.")
public interface IServiceProviderPlugin {

    /**
     *
     * Verify authentication ticket with the current service provider
     *
     * @param pAuthInformations
     *            Authentication informations
     * @return [true|false]
     * @since 1.0-SNAPSHOT
     */
    boolean checkTicketValidity(ExternalAuthenticationInformations pAuthInformations);

    /**
     *
     * Retrieve user informations with the current service provider
     *
     * @param pAuthInformations
     *            Authentication informations
     * @return {@link UserDetails}
     * @since 1.0-SNAPSHOT
     */
    UserDetails getUserInformations(ExternalAuthenticationInformations pAuthInformations);

}
