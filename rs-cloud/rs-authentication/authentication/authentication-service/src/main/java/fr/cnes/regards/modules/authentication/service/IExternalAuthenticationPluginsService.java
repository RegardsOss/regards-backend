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

import org.springframework.security.oauth2.common.OAuth2AccessToken;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.authentication.plugins.domain.ExternalAuthenticationInformations;

/**
 * Class IInternalAuthenticationPluginsService
 *
 * Internal authentication plugins Interface manager
 * @author Sébastien Binda
 */
public interface IExternalAuthenticationPluginsService {

    /**
     * Retrieve all configured Service Provider plugins to handle REGARDS internal authentication
     * @return List fo PluginConfiguration
     */
    List<PluginConfiguration> retrieveServiceProviderPlugins();

    /**
     * Retrieve a configured Service Provider plugin
     * @param pPluginConfigurationId PluginConfiguration identifier to retrieve
     * @return PluginConfiguration
     * @throws EntityNotFoundException Plugin does not exists
     */
    PluginConfiguration retrieveServiceProviderPlugin(Long pPluginConfigurationId) throws ModuleException;

    /**
     * Create a new Service Provider plugin
     * @param pPluginConfigurationToCreate PluginConfiguration to create
     * @return Created PluginConfiguration
     * @throws ModuleException Plugin to create is not valid
     */
    PluginConfiguration createServiceProviderPlugin(final PluginConfiguration pPluginConfigurationToCreate)
            throws ModuleException;

    /**
     * Update an Service Provider plugin
     * @param pPluginConfigurationToUpdate PluginConfiguration to update
     * @return updated PluginConfiguration (hateoas formated)
     * @throws ModuleException Plugin to update is not valid
     */
    PluginConfiguration updateServiceProviderPlugin(final PluginConfiguration pPluginConfigurationToUpdate)
            throws ModuleException;

    /**
     * Delete an Service Provider plugin
     * @param pluginBisnessId PluginConfiguration identifier to delete
     * @throws ModuleException Plugin to delete does not exists
     */
    void deleteServiceProviderPlugin(String pluginBisnessId) throws ModuleException;

    /**
     * Authenticate with the given Service Provider plugin.
     * @param pPluginConfigurationId Service Provider plugin to authenticate with
     * @param pAuthInformations External SSO informations to validate
     * @return OAuth2AccessToken
     */
    OAuth2AccessToken authenticate(Long pPluginConfigurationId, ExternalAuthenticationInformations pAuthInformations)
            throws EntityNotFoundException;

}
