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
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.authentication.plugins.IServiceProviderPlugin;
import fr.cnes.regards.modules.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * Class InternalAuthenticationPluginService
 *
 * Internal authentication plugins manager
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
@Service
public class ExternalAuthenticationPluginService implements IExternalAuthenticationPluginsService {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalAuthenticationPluginService.class);

    /**
     * Plugins service manager
     */
    private final IPluginService pluginService;

    /**
     * Security service
     */
    private final JWTService jwtService;

    /**
     * REST Client to administration service.
     */
    private final IProjectsClient projectsClient;

    /**
     * REST Client to administration service.
     */
    private final IProjectUsersClient projectUsersClient;

    /**
     * Constructor with attributes
     */
    public ExternalAuthenticationPluginService(final IPluginService pPluginService, final JWTService pJwtService,
            final IProjectsClient pProjectsClient, final IProjectUsersClient pProjectUsersClient) {
        super();
        pluginService = pPluginService;
        jwtService = pJwtService;
        projectsClient = pProjectsClient;
        projectUsersClient = pProjectUsersClient;
    }

    @Override
    public List<PluginConfiguration> retrieveServiceProviderPlugins() {
        return pluginService.getPluginConfigurationsByType(IServiceProviderPlugin.class);
    }

    @Override
    public PluginConfiguration retrieveServiceProviderPlugin(final Long pPluginConfigurationId) throws ModuleException {

        return pluginService.getPluginConfiguration(pPluginConfigurationId);
    }

    @Override
    public PluginConfiguration createServiceProviderPlugin(final PluginConfiguration pPluginConfigurationToCreate)
            throws ModuleException {
        return pluginService.savePluginConfiguration(pPluginConfigurationToCreate);
    }

    @Override
    public PluginConfiguration updateServiceProviderPlugin(final PluginConfiguration pPluginConfigurationToUpdate)
            throws ModuleException {
        return pluginService.updatePluginConfiguration(pPluginConfigurationToUpdate);
    }

    @Override
    public void deleteServiceProviderPlugin(String pluginBisnessId) throws ModuleException {
        pluginService.deletePluginConfiguration(pluginBisnessId);
    }

    @Override
    public OAuth2AccessToken authenticate(final Long pPluginConfigurationId,
            final ExternalAuthenticationInformations pAuthInformations) {
        try {
            // First check project existence
            final ResponseEntity<EntityModel<Project>> response = projectsClient
                    .retrieveProject(pAuthInformations.getProject());

            if (!HttpStatus.OK.equals(response.getStatusCode())) {
                throw new BadCredentialsException(
                        String.format("Project %s does not exists", pAuthInformations.getProject()));
            }

            // Then authenticate with given ServiceProviderPlugin
            final IServiceProviderPlugin plugin = pluginService.getPlugin(pPluginConfigurationId);
            if (plugin.checkTicketValidity(pAuthInformations)) {

                // Get informations about the user from the external service provider
                final UserDetails userDetails = plugin.getUserInformations(pAuthInformations);

                // Get informations about the user from the regards internal accounts.
                final ResponseEntity<EntityModel<ProjectUser>> userResponse = projectUsersClient
                        .retrieveProjectUserByEmail(userDetails.getEmail());

                if (userResponse.getStatusCode().equals(HttpStatus.OK)
                        && (userResponse.getBody().getContent() != null)) {
                    jwtService.generateToken(pAuthInformations.getProject(), pAuthInformations.getUserName(),
                                             userResponse.getBody().getContent().getEmail(),
                                             userResponse.getBody().getContent().getRole().getName());
                } else {
                    throw new BadCredentialsException(
                            String.format("User %s does not have access to project %s", userDetails.getLogin(),
                                          pAuthInformations.getProject()));
                }
            }
        } catch (final ModuleException e) {
            throw new BadCredentialsException(e.getMessage(), e);
        } catch (NotAvailablePluginConfigurationException e) {
            LOG.error(e.getMessage(), e);
        }

        return null;
    }
}
