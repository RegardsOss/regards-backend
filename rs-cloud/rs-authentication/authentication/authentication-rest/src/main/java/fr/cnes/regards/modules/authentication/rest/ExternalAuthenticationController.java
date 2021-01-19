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
package fr.cnes.regards.modules.authentication.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.authentication.plugins.IServiceProviderPlugin;
import fr.cnes.regards.modules.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.modules.authentication.service.IExternalAuthenticationPluginsService;

/**
 * Class InternalAuthenicationController
 *
 * REST Controller to manage internal authentication Service providers
 * @author Sébastien Binda
 */
@RestController
@RequestMapping("/authentication/sps")
public class ExternalAuthenticationController implements IResourceController<PluginConfiguration> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ExternalAuthenticationController.class);

    /**
     * Service to manage PluginConfiguration for internal authentication Service providers
     */
    private final IExternalAuthenticationPluginsService service;

    /**
     * Resource service to manage visibles hateoas links
     */
    private final IResourceService resourceService;

    public ExternalAuthenticationController(final IExternalAuthenticationPluginsService pService,
            final IResourceService pResourceService) {
        super();
        service = pService;
        resourceService = pResourceService;
    }

    /**
     * Retrieve all configured Service Provider plugins to handle REGARDS internal authentication
     * @return List fo PluginConfiguration (Hateoas formated)
     */
    @ResourceAccess(
            description = "Retrieve all configured Service Provider plugins to handle REGARDS internal authentication")
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<EntityModel<PluginConfiguration>>> retrieveServiceProviderPlugins() {
        final List<PluginConfiguration> plugins = service.retrieveServiceProviderPlugins();
        return new ResponseEntity<>(toResources(plugins), HttpStatus.OK);
    }

    /**
     * Create a new Service Provider plugin
     * @param pPluginConfigurationToCreate PluginConfiguration to create
     * @return Created PluginConfiguration (hateoas formated)
     */
    @ResourceAccess(description = "Create a new Service Provider plugin")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<EntityModel<PluginConfiguration>> createServiceProviderPlugin(
            @RequestBody final PluginConfiguration pPluginConfigurationToCreate) {
        ResponseEntity<EntityModel<PluginConfiguration>> response;
        try {
            final PluginConfiguration plugin = service.createServiceProviderPlugin(pPluginConfigurationToCreate);
            response = new ResponseEntity<>(toResource(plugin), HttpStatus.OK);
        } catch (final ModuleException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return response;
    }

    /**
     * Retrieve a configured Service Provider plugin
     * @param pPluginConfigurationId PluginConfiguration identifier to retrieve
     * @return PluginConfiguration (hateoas formated)
     */
    @ResourceAccess(description = "Retrieve a configured Service Provider plugin")
    @RequestMapping(path = "/{sp_id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<EntityModel<PluginConfiguration>> retrieveServiceProviderPlugin(
            @PathVariable("sp_id") final Long pPluginConfigurationId) {
        ResponseEntity<EntityModel<PluginConfiguration>> response;
        try {
            final PluginConfiguration plugin = service.retrieveServiceProviderPlugin(pPluginConfigurationId);
            if (plugin != null) {
                response = new ResponseEntity<>(toResource(plugin), HttpStatus.OK);
            } else {
                response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (final ModuleException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return response;
    }

    /**
     * Update an Service Provider plugin
     * @param pPluginConfigurationId PluginConfiguration identifier to update
     * @param pPluginConfigurationToUpdate PluginConfiguration to update
     * @return updated PluginConfiguration (hateoas formated)
     */
    @ResourceAccess(description = "Update a Service Provider plugin")
    @RequestMapping(path = "/{sp_id}", method = RequestMethod.PUT, produces = "application/json")
    public ResponseEntity<EntityModel<PluginConfiguration>> updateServiceProviderPlugin(
            @PathVariable("sp_id") final Long pPluginConfigurationId,
            @RequestBody final PluginConfiguration pPluginConfigurationToUpdate) {
        ResponseEntity<EntityModel<PluginConfiguration>> response;
        if (pPluginConfigurationId.equals(pPluginConfigurationToUpdate.getId())) {
            try {
                final PluginConfiguration plugin = service.updateServiceProviderPlugin(pPluginConfigurationToUpdate);
                if (plugin != null) {
                    response = new ResponseEntity<>(toResource(plugin), HttpStatus.OK);
                } else {
                    response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            } catch (final EntityNotFoundException e) {
                LOG.error(e.getMessage(), e);
                response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } catch (final ModuleException e) {
                LOG.error(e.getMessage(), e);
                response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

        } else {
            LOG.error(String.format("The given plugin id %s does not match the given plugin to update id %s",
                                    pPluginConfigurationId, pPluginConfigurationToUpdate.getId()));
            response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return response;
    }

    /**
     * Delete an Service Provider plugin
     * @param pPluginConfigurationId PluginConfiguration identifier to delete
     * @return Void
     */
    @ResourceAccess(description = "Delete a Service Provider plugin")
    @RequestMapping(path = "/{sp_id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteServiceProviderPlugin(@PathVariable("sp_id") final String pluginBisnessId) {
        ResponseEntity<Void> response;
        try {
            service.deleteServiceProviderPlugin(pluginBisnessId);
            response = new ResponseEntity<>(HttpStatus.OK);
        } catch (final ModuleException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return response;

    }

    /**
     * Authenticate with the given Service Provider plugin
     * @param pPluginConfigurationId PluginConfiguration identifier to delete
     * @param pAuthInformations informations use for connect throught external service provider
     * @return Void
     */
    @ResourceAccess(description = "Authenticate with the given Service Provider plugin",
            plugin = IServiceProviderPlugin.class)
    @RequestMapping(path = "/{sp_id}/authenticate", method = RequestMethod.POST)
    public ResponseEntity<OAuth2AccessToken> authenticate(@PathVariable("sp_id") final Long pPluginConfigurationId,
            @RequestBody final ExternalAuthenticationInformations pAuthInformations) {
        try {
            return new ResponseEntity<>(service.authenticate(pPluginConfigurationId, pAuthInformations), HttpStatus.OK);
        } catch (final EntityNotFoundException e) {
            LOG.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public EntityModel<PluginConfiguration> toResource(final PluginConfiguration pElement, final Object... pExtras) {
        final EntityModel<PluginConfiguration> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveServiceProviderPlugin", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "updateServiceProviderPlugin", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(PluginConfiguration.class, pElement));
        resourceService.addLink(resource, this.getClass(), "createServiceProviderPlugin", LinkRels.CREATE,
                                MethodParamFactory.build(PluginConfiguration.class, pElement));
        resourceService.addLink(resource, this.getClass(), "deleteServiceProviderPlugin", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        return resource;

    }

}
