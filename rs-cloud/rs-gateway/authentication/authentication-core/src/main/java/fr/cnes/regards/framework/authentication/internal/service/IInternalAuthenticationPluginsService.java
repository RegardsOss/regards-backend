/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.internal.service;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.InvalidEntityException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

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
     * @throws ModuleEntityNotFoundException
     *             Plugin does not exists
     * @since 1.0-SNAPSHOT
     */
    PluginConfiguration retrieveIdentityProviderPlugin(Long pPluginConfigurationId)
            throws ModuleEntityNotFoundException;

    /**
     *
     * Create a new Identity Provider plugin
     *
     * @param pPluginConfigurationToCreate
     *            PluginConfiguration to create
     * @return Created PluginConfiguration
     * @throws InvalidEntityException
     *             Plugin to create is not valid
     * @since 1.0-SNAPSHOT
     */
    PluginConfiguration createIdentityProviderPlugin(final PluginConfiguration pPluginConfigurationToCreate)
            throws InvalidEntityException;

    /**
     *
     * Update an Identity Provider plugin
     *
     * @param pPluginConfigurationToUpdate
     *            PluginConfiguration to update
     * @return updated PluginConfiguration (hateoas formated)
     * @throws InvalidEntityException
     *             Plugin to update is not valid
     * @throws ModuleEntityNotFoundException
     *             Plugin to update does not exists
     * @since 1.0-SNAPSHOT
     */
    PluginConfiguration updateIdentityProviderPlugin(final PluginConfiguration pPluginConfigurationToUpdate)
            throws InvalidEntityException, ModuleEntityNotFoundException;

    /**
     *
     * Delete an Identity Provider plugin
     *
     * @param pPluginConfigurationId
     *            PluginConfiguration identifier to delete
     * @throws PluginUtilsException
     *             Error deleting plugin
     * @throws ModuleEntityNotFoundException
     *             Plugin to delete does not exists
     * @since 1.0-SNAPSHOT
     */
    void deleteIdentityProviderPlugin(Long pPluginConfigurationId)
            throws ModuleEntityNotFoundException, PluginUtilsException;

}
