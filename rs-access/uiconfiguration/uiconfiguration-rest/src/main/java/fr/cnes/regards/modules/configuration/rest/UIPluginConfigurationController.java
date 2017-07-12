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
package fr.cnes.regards.modules.configuration.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
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
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.UIPluginConfiguration;
import fr.cnes.regards.modules.configuration.domain.UIPluginDefinition;
import fr.cnes.regards.modules.configuration.domain.UIPluginTypesEnum;
import fr.cnes.regards.modules.configuration.service.IUIPluginConfigurationService;

@RestController
@ModuleInfo(name = "Plugin", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(UIPluginConfigurationController.REQUEST_MAPPING_ROOT)
public class UIPluginConfigurationController implements IResourceController<UIPluginConfiguration> {

    public static final String REQUEST_MAPPING_ROOT = "/plugins";

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
     *
     * Endpoint to retrieve all {@link UIPluginConfiguration}
     *
     * @param pIsActive
     *            QueryParam Retrieve only the active {@link UIPluginConfiguration}
     * @param pIsLinkedToAllEntities
     *            QueryParam Retrieve only the pluginConfigurations linked to all entities
     * @param pAssembler
     *            Assembler to manage PagedResources.
     * @param pPageable
     *            Pagination parameters
     * @return Page {@link UIPluginConfiguration}
     * @throws EntityInvalidException
     *             error occurred.
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = REQUEST_PLUGIN_CONFIGURATIONS, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve all IHM plugin configurations", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedResources<Resource<UIPluginConfiguration>>> retrievePluginConfigurations(
            @RequestParam(value = "isActive", required = false) final Boolean pIsActive,
            @RequestParam(value = "isLinkedToAllEntities", required = false) final Boolean pIsLinkedToAllEntities,
            @RequestParam(value = "type", required = false) final UIPluginTypesEnum pPluginType,
            final PagedResourcesAssembler<UIPluginConfiguration> pAssembler, final Pageable pPageable)
            throws EntityInvalidException {
        final Page<UIPluginConfiguration> pluginConfs = service
                .retrievePluginConfigurations(pPluginType, pIsActive, pIsLinkedToAllEntities, pPageable);
        return new ResponseEntity<>(toPagedResources(pluginConfs, pAssembler), HttpStatus.OK);
    }

    /**
     *
     * Endpoint to retrieve all {@link UIPluginConfiguration} for a given plugin
     *
     * @param pPluginId
     *            Identifier of {@link UIPluginDefinition} to retrieve configurations
     * @param pIsActive
     *            QueryParam Retrieve only the active pluginConfiguration
     * @param pIsLinkedToAllEntities
     *            QueryParam Retrieve only the pluginConfigurations linked to all entities
     * @param pAssembler
     *            Assembler to manage PagedResources.
     * @param pPageable
     *            Pagination parameters
     * @return Page {@link UIPluginConfiguration}
     * @throws EntityException
     *             error occurred.
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = REQUEST_PLUGIN_DEFINITION, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM plugin for a given PluginDefinition",
            role = DefaultRole.PUBLIC)
    public HttpEntity<PagedResources<Resource<UIPluginConfiguration>>> retrievePluginConfigurationsByPlugin(
            @PathVariable("pluginId") final Long pPluginId,
            @RequestParam(value = "isActive", required = false) final Boolean pIsActive,
            @RequestParam(value = "isLinkedToAllEntities", required = false) final Boolean pIsLinkedToAllEntities,
            final PagedResourcesAssembler<UIPluginConfiguration> pAssembler, final Pageable pPageable)
            throws EntityException {
        final UIPluginDefinition plugin = new UIPluginDefinition();
        plugin.setId(pPluginId);
        final Page<UIPluginConfiguration> pluginConfs = service
                .retrievePluginConfigurations(plugin, pIsActive, pIsLinkedToAllEntities, pPageable);
        return new ResponseEntity<>(toPagedResources(pluginConfs, pAssembler), HttpStatus.OK);
    }

    /**
     *
     * Endpoint to retrieve one {@link UIPluginConfiguration} by his identifier.
     *
     * @param pPluginConfigurationId
     *            {@link UIPluginConfiguration} identifier
     * @return {@lunk PluginConfiguration}
     * @throws EntityInvalidException
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = REQUEST_PLUGIN_CONFIGURATION, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM plugin", role = DefaultRole.PUBLIC)
    public HttpEntity<Resource<UIPluginConfiguration>> retrievePluginConfiguration(
            @PathVariable("pluginConfId") final Long pPluginConfigurationId) throws EntityInvalidException {
        final UIPluginConfiguration pluginConf = service.retrievePluginconfiguration(pPluginConfigurationId);
        return new ResponseEntity<>(toResource(pluginConf), HttpStatus.OK);
    }

    /**
     *
     * Endpoint to update a {@link UIPluginConfiguration} by his identifier.
     *
     * @param pPluginConfigurationId
     *            {@link UIPluginConfiguration} identifier
     * @param {@link
     *            UIPluginConfiguration} to update
     * @return {@lunk PluginConfiguration}
     * @throws EntityInvalidException
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = REQUEST_PLUGIN_CONFIGURATION, method = RequestMethod.PUT)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to update an IHM plugin configuration", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<UIPluginConfiguration>> updatePluginConfiguration(
            @PathVariable("pluginConfId") final Long pPluginConfigurationId,
            @Valid @RequestBody final UIPluginConfiguration pPluginConfiguration) throws EntityException {
        if ((pPluginConfigurationId == null) || !pPluginConfigurationId.equals(pPluginConfiguration.getId())) {
            throw new EntityInvalidException(String.format("Invalid entity id %s", pPluginConfigurationId));
        }
        final UIPluginConfiguration pluginConf = service.updatePluginconfiguration(pPluginConfiguration);
        return new ResponseEntity<>(toResource(pluginConf), HttpStatus.OK);
    }

    /**
     *
     * Endpoint to create a new {@link UIPluginConfiguration}.
     *
     * @param pPluginConfigurationId
     *            {@link UIPluginConfiguration} identifier
     * @param {@link
     *            UIPluginConfiguration} to update
     * @return {@lunk PluginConfiguration}
     * @throws EntityInvalidException
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = REQUEST_PLUGIN_CONFIGURATIONS, method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM plugin configuration", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<UIPluginConfiguration>> createPluginConfiguration(
            @Valid @RequestBody final UIPluginConfiguration pPluginConfiguration) throws EntityException {
        final UIPluginConfiguration pluginConf = service.createPluginconfiguration(pPluginConfiguration);
        return new ResponseEntity<>(toResource(pluginConf), HttpStatus.OK);
    }

    /**
     *
     * Endpoint to delete a {@link UIPluginConfiguration}.
     *
     * @param pPluginConfigurationId
     *            {@link UIPluginConfiguration} identifier to delete
     * @return {@lunk PluginConfiguration}
     * @throws EntityInvalidException
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = REQUEST_PLUGIN_CONFIGURATION, method = RequestMethod.DELETE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to delete an IHM plugin configuration", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Void>> deletePluginConfiguration(
            @PathVariable("pluginConfId") final Long pPluginConfigurationId) throws EntityException {
        final UIPluginConfiguration pluginConfToDelete = new UIPluginConfiguration();
        pluginConfToDelete.setId(pPluginConfigurationId);
        service.deletePluginconfiguration(pluginConfToDelete);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<UIPluginConfiguration> toResource(final UIPluginConfiguration pElement, final Object... pExtras) {
        final Resource<UIPluginConfiguration> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrievePluginConfiguration", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "updatePluginConfiguration", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(UIPluginDefinition.class));
        resourceService.addLink(resource, this.getClass(), "deletePluginConfiguration", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        return resource;
    }

    public Resource<UIPluginConfiguration> servicesToResource(final UIPluginConfiguration pElement) {
        final Resource<UIPluginConfiguration> resource = resourceService.toResource(pElement);
        return resource;
    }

}
