/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.rest.ui;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;
import fr.cnes.regards.modules.access.services.service.ui.IUIPluginDefinitionService;

/**
 * REST controller for the microservice Access
 *
 * @author Sébastien Binda
 *
 */
@RestController
@ModuleInfo(name = "Plugin", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(UIPluginDefinitionController.REQUEST_MAPPING_ROOT)
public class UIPluginDefinitionController implements IResourceController<UIPluginDefinition> {

    /**
     * Root request mapping
     */
    public static final String REQUEST_MAPPING_ROOT = "/uiplugins/definition";

    public static final String REQUEST_MAPPING_PLUGIN_DEFINITION = "/{pluginId}";

    @Autowired
    private IUIPluginDefinitionService service;

    @Autowired
    private IResourceService resourceService;

    /**
     * Entry point to retrieve a plugins {@link UIPluginDefinition}.
     *
     * @return {@link UIPluginDefinition}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = REQUEST_MAPPING_PLUGIN_DEFINITION, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM plugin", role = DefaultRole.PUBLIC)
    public HttpEntity<Resource<UIPluginDefinition>> retrievePlugin(@PathVariable("pluginId") final Long pPluginId)
            throws EntityNotFoundException {
        final UIPluginDefinition plugin = service.retrievePlugin(pPluginId);
        return new ResponseEntity<>(toResource(plugin), HttpStatus.OK);
    }

    /**
     * Entry point to retrieve all plugins
     *
     * @return {@link UIPluginDefinition}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve all IHM plugins", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedResources<Resource<UIPluginDefinition>>> retrievePlugins(final Pageable pPageable,
            @RequestParam(value = "type", required = false) final UIPluginTypesEnum pType,
            final PagedResourcesAssembler<UIPluginDefinition> pAssembler) throws EntityInvalidException {

        final Page<UIPluginDefinition> plugins;
        if (pType != null) {
            plugins = service.retrievePlugins(pType, pPageable);
        } else {
            plugins = service.retrievePlugins(pPageable);
        }
        final PagedResources<Resource<UIPluginDefinition>> resources = toPagedResources(plugins, pAssembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Entry point to save a new plugin
     *
     * @return {@link UIPluginDefinition}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM plugin", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<UIPluginDefinition>> savePlugin(@Valid @RequestBody final UIPluginDefinition pPlugin)
            throws EntityInvalidException {
        final UIPluginDefinition plugin = service.savePlugin(pPlugin);
        final Resource<UIPluginDefinition> resource = toResource(plugin);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to save a new ihm plugin.
     *
     * @return {@link UIPluginDefinition}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = REQUEST_MAPPING_PLUGIN_DEFINITION, method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to update an IHM plugin", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<UIPluginDefinition>> updatePlugin(@PathVariable("pluginId") final Long pPluginId,
            @Valid @RequestBody final UIPluginDefinition pPlugin) throws EntityException {

        if (!pPlugin.getId().equals(pPluginId)) {
            throw new EntityInvalidException("Invalide application identifier for plugin");
        }
        final UIPluginDefinition plugin = service.updatePlugin(pPlugin);
        final Resource<UIPluginDefinition> resource = toResource(plugin);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to delete an ihm plugin.
     *
     * @return {@link UIPluginDefinition}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = REQUEST_MAPPING_PLUGIN_DEFINITION, method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to delete a plugin", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Void>> deletePlugin(@PathVariable("pluginId") final Long pPluginId)
            throws EntityNotFoundException {
        service.deletePlugin(pPluginId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<UIPluginDefinition> toResource(final UIPluginDefinition pElement, final Object... pExtras) {
        final Resource<UIPluginDefinition> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrievePlugin", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "updatePlugin", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(UIPluginDefinition.class));
        resourceService.addLink(resource, this.getClass(), "deletePlugin", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        return resource;
    }

}
