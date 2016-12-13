/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.external.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IServiceProviderPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 *
 * Class InternalAuthenticationPluginService
 *
 * Internal authentication plugins manager
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@Service
public class ExternalAuthenticationPluginService implements IExternalAuthenticationPluginsService {

    /**
     * Class logger
     */
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
     * @param pPluginService
     * @param pJwtService
     * @param pProjectsClient
     * @param pProjectUsersClient
     */
    public ExternalAuthenticationPluginService(final IPluginService pPluginService, final JWTService pJwtService,
            final IProjectsClient pProjectsClient, final IProjectUsersClient pProjectUsersClient) {
        super();
        pluginService = pPluginService;
        jwtService = pJwtService;
        projectsClient = pProjectsClient;
        projectUsersClient = pProjectUsersClient;
    }

    @PostConstruct
    public void addPluginPackage() {
        pluginService.addPluginPackage("fr.cnes.regards.cloud.gateway.authentication.plugins");
    }

    @Override
    public List<PluginConfiguration> retrieveServiceProviderPlugins() {
        return pluginService.getPluginConfigurationsByType(IServiceProviderPlugin.class);
    }

    @Override
    public PluginConfiguration retrieveServiceProviderPlugin(final Long pPluginConfigurationId)
            throws EntityNotFoundException {
        try {
            return pluginService.getPluginConfiguration(pPluginConfigurationId);
        } catch (final PluginUtilsException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityNotFoundException(pPluginConfigurationId.toString(), PluginConfiguration.class);
        }
    }

    @Override
    public PluginConfiguration createServiceProviderPlugin(final PluginConfiguration pPluginConfigurationToCreate)
            throws ModuleException {
        try {
            return pluginService.savePluginConfiguration(pPluginConfigurationToCreate);
        } catch (final PluginUtilsException e) {
            LOG.error(e.getMessage(), e);
            throw new ModuleException(e.getMessage());
        }

    }

    @Override
    public PluginConfiguration updateServiceProviderPlugin(final PluginConfiguration pPluginConfigurationToUpdate)
            throws ModuleException {
        try {
            return pluginService.updatePluginConfiguration(pPluginConfigurationToUpdate);
        } catch (final PluginUtilsException e) {
            LOG.error(e.getMessage(), e);
            throw new ModuleException(e.getMessage());
        }
    }

    @Override
    public void deleteServiceProviderPlugin(final Long pPluginConfigurationId) throws EntityNotFoundException {
        pluginService.deletePluginConfiguration(pPluginConfigurationId);
    }

    @Override
    public OAuth2AccessToken authenticate(final Long pPluginConfigurationId,
            final ExternalAuthenticationInformations pAuthInformations) throws EntityNotFoundException {
        try {
            // First check project existence
            final ResponseEntity<Resource<Project>> response = projectsClient
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
                final ResponseEntity<Resource<ProjectUser>> userResponse = projectUsersClient
                        .retrieveProjectUser(userDetails.getName());

                if (userResponse.getStatusCode().equals(HttpStatus.OK)
                        && (userResponse.getBody().getContent() != null)) {
                    jwtService.generateToken(pAuthInformations.getProject(),
                                             userResponse.getBody().getContent().getEmail(),
                                             userResponse.getBody().getContent().getRole().getName());
                } else {
                    throw new BadCredentialsException(
                            String.format("User %s does not have access to project %s", userDetails.getName(),
                                          pAuthInformations.getProject()));
                }
            }

        } catch (final PluginUtilsException e) {
            throw new BadCredentialsException(e.getMessage(), e);
        }

        return null;
    }

}
