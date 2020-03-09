/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
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
     * @param pluginId
     *
     * @return {@link UIPluginDefinition}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = REQUEST_MAPPING_PLUGIN_DEFINITION, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM plugin", role = DefaultRole.PUBLIC)
    public HttpEntity<Resource<UIPluginDefinition>> retrievePlugin(@PathVariable("pluginId") final Long pluginId)
            throws EntityNotFoundException {
        final UIPluginDefinition plugin = service.retrievePlugin(pluginId);
        return new ResponseEntity<>(toResource(plugin), HttpStatus.OK);
    }

    /**
     * Entry point to retrieve all plugins
     * @param pageable
     * @param type
     * @param assembler
     *
     * @return {@link UIPluginDefinition}
     * @throws EntityInvalidException
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve all IHM plugins", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedResources<Resource<UIPluginDefinition>>> retrievePlugins(
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(value = "type", required = false) final UIPluginTypesEnum type,
            final PagedResourcesAssembler<UIPluginDefinition> assembler) throws EntityInvalidException {

        final Page<UIPluginDefinition> plugins;
        if (type != null) {
            plugins = service.retrievePlugins(type, pageable);
        } else {
            plugins = service.retrievePlugins(pageable);
        }
        final PagedResources<Resource<UIPluginDefinition>> resources = toPagedResources(plugins, assembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Entry point to save a new plugin
     * @param inPlugin
     *
     * @return {@link UIPluginDefinition}
     * @throws EntityInvalidException
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM plugin", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<UIPluginDefinition>> savePlugin(@Valid @RequestBody final UIPluginDefinition inPlugin)
            throws EntityInvalidException {
        final UIPluginDefinition plugin = service.savePlugin(inPlugin);
        final Resource<UIPluginDefinition> resource = toResource(plugin);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to save a new ihm plugin.
     * @param pluginId
     * @param inPlugin
     *
     * @return {@link UIPluginDefinition}
     * @throws EntityException
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = REQUEST_MAPPING_PLUGIN_DEFINITION, method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to update an IHM plugin", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<UIPluginDefinition>> updatePlugin(@PathVariable("pluginId") final Long pluginId,
            @Valid @RequestBody final UIPluginDefinition inPlugin) throws EntityException {

        if (!inPlugin.getId().equals(pluginId)) {
            throw new EntityInvalidException("Invalide application identifier for plugin");
        }
        final UIPluginDefinition plugin = service.updatePlugin(inPlugin);
        final Resource<UIPluginDefinition> resource = toResource(plugin);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to delete an ihm plugin.
     * @param pluginId
     *
     * @return {@link UIPluginDefinition}
     * @throws ModuleException
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = REQUEST_MAPPING_PLUGIN_DEFINITION, method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to delete a plugin", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Void>> deletePlugin(@PathVariable("pluginId") final Long pluginId)
            throws ModuleException {
        service.deletePlugin(pluginId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<UIPluginDefinition> toResource(final UIPluginDefinition element, final Object... extras) {
        final Resource<UIPluginDefinition> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrievePlugin", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource, this.getClass(), "updatePlugin", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, element.getId()),
                                MethodParamFactory.build(UIPluginDefinition.class));
        resourceService.addLink(resource, this.getClass(), "deletePlugin", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, element.getId()));
        return resource;
    }

}
