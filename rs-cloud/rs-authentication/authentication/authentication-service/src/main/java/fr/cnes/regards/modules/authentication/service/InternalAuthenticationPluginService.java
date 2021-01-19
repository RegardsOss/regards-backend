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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.authentication.plugins.IAuthenticationPlugin;

/**
 * Class InternalAuthenticationPluginService
 *
 * Internal authentication plugins manager
 * @author Sébastien Binda
 */
@Service
public class InternalAuthenticationPluginService implements IInternalAuthenticationPluginsService {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalAuthenticationPluginService.class);

    /**
     * Plugins service manager
     */
    private final IPluginService pluginService;

    public InternalAuthenticationPluginService(final IPluginService pPluginService) {
        super();
        pluginService = pPluginService;
    }

    @Override
    public List<PluginConfiguration> retrieveIdentityProviderPlugins() {
        return pluginService.getPluginConfigurationsByType(IAuthenticationPlugin.class);
    }

    @Override
    public PluginConfiguration retrieveIdentityProviderPlugin(final Long pPluginConfigurationId)
            throws ModuleException {

        return pluginService.getPluginConfiguration(pPluginConfigurationId);
    }

    @Override
    public PluginConfiguration createIdentityProviderPlugin(final PluginConfiguration pPluginConfigurationToCreate)
            throws ModuleException {
        return pluginService.savePluginConfiguration(pPluginConfigurationToCreate);
    }

    @Override
    public PluginConfiguration updateIdentityProviderPlugin(final PluginConfiguration pPluginConfigurationToUpdate)
            throws ModuleException {
        return pluginService.updatePluginConfiguration(pPluginConfigurationToUpdate);
    }

    @Override
    public void deleteIdentityProviderPlugin(final String pluginBisnessId) throws ModuleException {
        pluginService.deletePluginConfiguration(pluginBisnessId);
    }

}
