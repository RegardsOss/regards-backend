/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;
import fr.cnes.regards.modules.access.services.service.ui.IUIPluginConfigurationService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller managing {@link UIPluginConfiguration}s
 */
@RestController
@RequestMapping(UIPluginConfigurationController.REQUEST_MAPPING_ROOT)
public class UIPluginConfigurationController implements IResourceController<UIPluginConfiguration> {

    public static final String REQUEST_MAPPING_ROOT = "/uiplugins";

    public static final String REQUEST_PLUGIN_CONFIGURATIONS = "/configurations";

    public static final String REQUEST_PLUGIN_CONFIGURATION = "/configurations/{pluginConfId}";

    public static final String REQUEST_PLUGIN_DEFINITION = "/{pluginId}/configurations";

    /**
     * Business service to manage {@link UIPluginConfiguration} entities
     */
    @Autowired
    private IUIPluginConfigurationService service;

    /**
     * Service to manage hateoas resources
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Endpoint to retrieve all {@link UIPluginConfiguration}
     *
     * @param isActive              QueryParam Retrieve only the active {@link UIPluginConfiguration}
     * @param isLinkedToAllEntities QueryParam Retrieve only the pluginConfigurations linked to all entities
     * @param pluginType            The plugin type
     * @param assembler             Assembler to manage PagedResources.
     * @param pageable              Pagination parameters
     * @return Page {@link UIPluginConfiguration}
     */
    @RequestMapping(value = REQUEST_PLUGIN_CONFIGURATIONS, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve all IHM plugin configurations", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedModel<EntityModel<UIPluginConfiguration>>> retrievePluginConfigurations(
        @RequestParam(value = "isActive", required = false) final Boolean isActive,
        @RequestParam(value = "isLinkedToAllEntities", required = false) final Boolean isLinkedToAllEntities,
        @RequestParam(value = "type", required = false) final UIPluginTypesEnum pluginType,
        @Parameter(hidden = true) final PagedResourcesAssembler<UIPluginConfiguration> assembler,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        final Page<UIPluginConfiguration> pluginConfs = service.retrievePluginConfigurations(pluginType,
                                                                                             isActive,
                                                                                             isLinkedToAllEntities,
                                                                                             pageable);
        return new ResponseEntity<>(toPagedResources(pluginConfs, assembler), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve all {@link UIPluginConfiguration} for a given plugin
     *
     * @param pPluginId             Identifier of {@link UIPluginDefinition} to retrieve configurations
     * @param isActive              QueryParam Retrieve only the active pluginConfiguration
     * @param isLinkedToAllEntities QueryParam Retrieve only the pluginConfigurations linked to all entities
     * @param assembler             Assembler to manage PagedResources.
     * @param pageable              Pagination parameters
     * @return Page {@link UIPluginConfiguration}
     * @throws EntityException error occurred.
     */
    @RequestMapping(value = REQUEST_PLUGIN_DEFINITION, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM plugin for a given PluginDefinition",
                    role = DefaultRole.PUBLIC)
    public HttpEntity<PagedModel<EntityModel<UIPluginConfiguration>>> retrievePluginConfigurationsByPlugin(
        @PathVariable("pluginId") final Long pPluginId,
        @RequestParam(value = "isActive", required = false) final Boolean isActive,
        @RequestParam(value = "isLinkedToAllEntities", required = false) final Boolean isLinkedToAllEntities,
        @Parameter(hidden = true) final PagedResourcesAssembler<UIPluginConfiguration> assembler,
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) throws EntityException {
        final UIPluginDefinition plugin = new UIPluginDefinition();
        plugin.setId(pPluginId);
        final Page<UIPluginConfiguration> pluginConfs = service.retrievePluginConfigurations(plugin,
                                                                                             isActive,
                                                                                             isLinkedToAllEntities,
                                                                                             pageable);
        return new ResponseEntity<>(toPagedResources(pluginConfs, assembler), HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve one {@link UIPluginConfiguration} by his identifier.
     *
     * @param pluginConfigurationId {@link UIPluginConfiguration} identifier
     * @return {@lunk PluginConfiguration}
     */
    @RequestMapping(value = REQUEST_PLUGIN_CONFIGURATION, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM plugin", role = DefaultRole.PUBLIC)
    public HttpEntity<EntityModel<UIPluginConfiguration>> retrievePluginConfiguration(
        @PathVariable("pluginConfId") final Long pluginConfigurationId) throws EntityInvalidException {
        final UIPluginConfiguration pluginConf = service.retrievePluginconfiguration(pluginConfigurationId);
        return new ResponseEntity<>(toResource(pluginConf), HttpStatus.OK);
    }

    /**
     * Endpoint to update a {@link UIPluginConfiguration} by his identifier.
     *
     * @param pluginConfigurationId {@link UIPluginConfiguration} identifier
     * @return {@link UIPluginConfiguration} to update
     */
    @RequestMapping(value = REQUEST_PLUGIN_CONFIGURATION, method = RequestMethod.PUT)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to update an IHM plugin configuration", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<UIPluginConfiguration>> updatePluginConfiguration(
        @PathVariable("pluginConfId") final Long pluginConfigurationId,
        @Valid @RequestBody final UIPluginConfiguration pluginConfiguration) throws EntityException {
        if ((pluginConfigurationId == null) || !pluginConfigurationId.equals(pluginConfiguration.getId())) {
            throw new EntityInvalidException(String.format("Invalid entity id %s", pluginConfigurationId));
        }
        final UIPluginConfiguration pluginConf = service.updatePluginconfiguration(pluginConfiguration);
        return new ResponseEntity<>(toResource(pluginConf), HttpStatus.OK);
    }

    /**
     * Endpoint to create a new {@link UIPluginConfiguration}.
     *
     * @return {@link UIPluginConfiguration}
     */
    @RequestMapping(value = REQUEST_PLUGIN_CONFIGURATIONS, method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM plugin configuration", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<UIPluginConfiguration>> createPluginConfiguration(
        @Valid @RequestBody final UIPluginConfiguration pluginConfiguration) throws EntityException {
        final UIPluginConfiguration pluginConf = service.createPluginconfiguration(pluginConfiguration);
        return new ResponseEntity<>(toResource(pluginConf), HttpStatus.OK);
    }

    /**
     * Endpoint to delete a {@link UIPluginConfiguration}.
     *
     * @param pluginConfigurationId {@link UIPluginConfiguration} identifier to delete
     * @return {@lunk PluginConfiguration}
     */
    @RequestMapping(value = REQUEST_PLUGIN_CONFIGURATION, method = RequestMethod.DELETE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to delete an IHM plugin configuration", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<Void>> deletePluginConfiguration(
        @PathVariable("pluginConfId") final Long pluginConfigurationId) throws EntityException {
        final UIPluginConfiguration pluginConfToDelete = new UIPluginConfiguration();
        pluginConfToDelete.setId(pluginConfigurationId);
        service.deletePluginconfiguration(pluginConfToDelete);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<UIPluginConfiguration> toResource(final UIPluginConfiguration element, final Object... extras) {
        final EntityModel<UIPluginConfiguration> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrievePluginConfiguration",
                                LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "updatePluginConfiguration",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, element.getId()),
                                MethodParamFactory.build(UIPluginConfiguration.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "deletePluginConfiguration",
                                LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, element.getId()));
        return resource;
    }

    /**
     * Convert services to resources
     *
     * @return {@link UIPluginConfiguration}
     */
    public EntityModel<UIPluginConfiguration> servicesToResource(final UIPluginConfiguration element) {
        return resourceService.toResource(element);
    }

}
