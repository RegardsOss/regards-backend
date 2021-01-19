/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.authentication.service;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

/**
 * Class IInternalAuthenticationPluginsService
 *
 * Internal authentication plugins Interface manager
 * @author Sébastien Binda
 */
public interface IInternalAuthenticationPluginsService {

    /**
     * Retrieve all configured Identity Provider plugins to handle REGARDS internal authentication
     * @return List fo PluginConfiguration
     */
    List<PluginConfiguration> retrieveIdentityProviderPlugins();

    /**
     * Retrieve a configured Identity Provider plugin
     * @param pPluginConfigurationId PluginConfiguration identifier to retrieve
     * @return PluginConfiguration
     * @throws ModuleException Plugin does not exists
     */
    PluginConfiguration retrieveIdentityProviderPlugin(Long pPluginConfigurationId) throws ModuleException;

    /**
     * Create a new Identity Provider plugin
     * @param pPluginConfigurationToCreate PluginConfiguration to create
     * @return Created PluginConfiguration
     * @throws ModuleException Plugin to create is not valid
     */
    PluginConfiguration createIdentityProviderPlugin(final PluginConfiguration pPluginConfigurationToCreate)
            throws ModuleException;

    /**
     * Update an Identity Provider plugin
     * @param pPluginConfigurationToUpdate PluginConfiguration to update
     * @return updated PluginConfiguration (hateoas formated)
     * @throws ModuleException Plugin to update does not exists
     */
    PluginConfiguration updateIdentityProviderPlugin(final PluginConfiguration pPluginConfigurationToUpdate)
            throws ModuleException;

    /**
     * Delete an Identity Provider plugin
     * @param pluginBisnessId PluginConfiguration identifier to delete
     * @throws ModuleException Plugin to delete does not exists
     */
    void deleteIdentityProviderPlugin(String pluginBisnessId) throws ModuleException;

}
