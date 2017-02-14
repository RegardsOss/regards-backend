/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.external.service;

import java.util.List;

import org.springframework.security.oauth2.common.OAuth2AccessToken;

import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;


/**
 *
 * Class IInternalAuthenticationPluginsService
 *
 * Internal authentication plugins Interface manager
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public interface IExternalAuthenticationPluginsService {

    /**
     *
     * Retrieve all configured Service Provider plugins to handle REGARDS internal authentication
     *
     * @return List fo PluginConfiguration
     * @since 1.0-SNAPSHOT
     */
    List<PluginConfiguration> retrieveServiceProviderPlugins();

    /**
     *
     * Retrieve a configured Service Provider plugin
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration identifier to retrieve
     * @return PluginConfiguration
     * @throws EntityNotFoundException
     *             Plugin does not exists
     * @since 1.0-SNAPSHOT
     */
    PluginConfiguration retrieveServiceProviderPlugin(Long pPluginConfigurationId) throws ModuleException;

    /**
     *
     * Create a new Service Provider plugin
     *
     * @param pPluginConfigurationToCreate
     *            PluginConfiguration to create
     * @return Created PluginConfiguration
     * @throws ModuleException
     *             Plugin to create is not valid
     * @since 1.0-SNAPSHOT
     */
    PluginConfiguration createServiceProviderPlugin(final PluginConfiguration pPluginConfigurationToCreate)
            throws ModuleException;

    /**
     *
     * Update an Service Provider plugin
     *
     * @param pPluginConfigurationToUpdate
     *            PluginConfiguration to update
     * @return updated PluginConfiguration (hateoas formated)
     * @throws ModuleException
     *             Plugin to update is not valid
     * @since 1.0-SNAPSHOT
     */
    PluginConfiguration updateServiceProviderPlugin(final PluginConfiguration pPluginConfigurationToUpdate)
            throws ModuleException;

    /**
     *
     * Delete an Service Provider plugin
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration identifier to delete
     * @throws ModuleException
     *             Plugin to delete does not exists
     * @since 1.0-SNAPSHOT
     */
    void deleteServiceProviderPlugin(Long pPluginConfigurationId) throws ModuleException;

    /**
     *
     * Authenticate with the given Service Provider plugin.
     *
     * @param pPluginConfigurationId
     *            Service Provider plugin to authenticate with
     * @param pAuthInformations
     *            External SSO informations to validate
     * @return OAuth2AccessToken
     * @throws EntityNotFoundException
     * @since 1.0-SNAPSHOT
     */
    OAuth2AccessToken authenticate(Long pPluginConfigurationId, ExternalAuthenticationInformations pAuthInformations)
            throws EntityNotFoundException;

}
