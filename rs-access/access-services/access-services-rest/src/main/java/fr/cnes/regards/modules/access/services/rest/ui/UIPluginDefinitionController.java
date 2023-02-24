/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST controller for the microservice Access
 *
 * @author Sébastien Binda
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
     *
     * @return {@link UIPluginDefinition}
     */
    @RequestMapping(value = REQUEST_MAPPING_PLUGIN_DEFINITION,
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM plugin", role = DefaultRole.PUBLIC)
    public HttpEntity<EntityModel<UIPluginDefinition>> retrievePlugin(@PathVariable("pluginId") final Long pluginId)
        throws EntityNotFoundException {
        final UIPluginDefinition plugin = service.retrievePlugin(pluginId);
        return new ResponseEntity<>(toResource(plugin), HttpStatus.OK);
    }

    /**
     * Entry point to retrieve all UI plugins definitions
     *
     * @return {@link UIPluginDefinition}
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get UI Plugin definitions", description = "Return a page of UI Plugin definitions")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All IHM plugins were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve UI Plugin definition", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedModel<EntityModel<UIPluginDefinition>>> retrievePlugins(
        @RequestParam(value = "type", required = false) final UIPluginTypesEnum type,
        @PageableDefault(sort = "name", direction = Sort.Direction.ASC, size = 500) Pageable pageable,
        final PagedResourcesAssembler<UIPluginDefinition> assembler) {

        Page<UIPluginDefinition> plugins;
        if (type != null) {
            plugins = service.retrievePlugins(type, pageable);
        } else {
            plugins = service.retrievePlugins(pageable);
        }

        return new ResponseEntity<>(toPagedResources(plugins, assembler), HttpStatus.OK);
    }

    /**
     * Entry point to save a new plugin
     *
     * @return {@link UIPluginDefinition}
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM plugin", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<UIPluginDefinition>> savePlugin(@Valid @RequestBody final UIPluginDefinition inPlugin)
        throws EntityInvalidException {
        final UIPluginDefinition plugin = service.savePlugin(inPlugin);
        final EntityModel<UIPluginDefinition> resource = toResource(plugin);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to save a new ihm plugin.
     *
     * @return {@link UIPluginDefinition}
     */
    @RequestMapping(value = REQUEST_MAPPING_PLUGIN_DEFINITION,
                    method = RequestMethod.PUT,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to update an IHM plugin", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<UIPluginDefinition>> updatePlugin(@PathVariable("pluginId") final Long pluginId,
                                                                    @Valid @RequestBody
                                                                    final UIPluginDefinition inPlugin)
        throws EntityException {

        if (!inPlugin.getId().equals(pluginId)) {
            throw new EntityInvalidException("Invalid application identifier for plugin");
        }
        final UIPluginDefinition plugin = service.updatePlugin(inPlugin);
        final EntityModel<UIPluginDefinition> resource = toResource(plugin);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to delete an ihm plugin.
     *
     * @return {@link UIPluginDefinition}
     */
    @RequestMapping(value = REQUEST_MAPPING_PLUGIN_DEFINITION,
                    method = RequestMethod.DELETE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to delete a plugin", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<Void>> deletePlugin(@PathVariable("pluginId") final Long pluginId)
        throws ModuleException {
        service.deletePlugin(pluginId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<UIPluginDefinition> toResource(final UIPluginDefinition element, final Object... extras) {
        final EntityModel<UIPluginDefinition> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrievePlugin",
                                LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "updatePlugin",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, element.getId()),
                                MethodParamFactory.build(UIPluginDefinition.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "deletePlugin",
                                LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, element.getId()));
        return resource;
    }

}
