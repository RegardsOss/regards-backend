/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.internal.service;

import java.util.List;

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
public interface IInternalAuthenticationPluginsService {

    /**
     *
     * Retrieve all configured Identity Provider plugins to handle REGARDS internal authentication
     *
     * @return List fo PluginConfiguration
     * @since 1.0-SNAPSHOT
     */
    List<PluginConfiguration> retrieveIdentityProviderPlugins();

    /**
     *
     * Retrieve a configured Identity Provider plugin
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration identifier to retrieve
     * @return PluginConfiguration
     * @throws EntityNotFoundException
     *             Plugin does not exists
     * @since 1.0-SNAPSHOT
     */
    PluginConfiguration retrieveIdentityProviderPlugin(Long pPluginConfigurationId) throws EntityNotFoundException;

    /**
     *
     * Create a new Identity Provider plugin
     *
     * @param pPluginConfigurationToCreate
     *            PluginConfiguration to create
     * @return Created PluginConfiguration
     * @throws ModuleException
     *             Plugin to create is not valid
     * @since 1.0-SNAPSHOT
     */
    PluginConfiguration createIdentityProviderPlugin(final PluginConfiguration pPluginConfigurationToCreate)
            throws ModuleException;

    /**
     *
     * Update an Identity Provider plugin
     *
     * @param pPluginConfigurationToUpdate
     *            PluginConfiguration to update
     * @return updated PluginConfiguration (hateoas formated)
     * @throws ModuleException
     *             Plugin to update does not exists
     * @since 1.0-SNAPSHOT
     */
    PluginConfiguration updateIdentityProviderPlugin(final PluginConfiguration pPluginConfigurationToUpdate)
            throws ModuleException;

    /**
     *
     * Delete an Identity Provider plugin
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration identifier to delete
     * @throws EntityNotFoundException
     *             Plugin to delete does not exists
     * @since 1.0-SNAPSHOT
     */
    void deleteIdentityProviderPlugin(Long pPluginConfigurationId) throws EntityNotFoundException;

}
