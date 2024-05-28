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
package fr.cnes.regards.modules.search.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineConfiguration;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import fr.cnes.regards.modules.search.service.ISearchEngineConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for {@link SearchEngineConfiguration}s CRUD
 *
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(path = SearchEngineConfigurationController.TYPE_MAPPING)
public class SearchEngineConfigurationController implements IResourceController<SearchEngineConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineConfigurationController.class);

    public static final String TYPE_MAPPING = "/enginesconfig";

    public static final String CONF_ID_PARAMETER_NAME = "confId";

    public static final String CONF_ID_PATH = "/{" + CONF_ID_PARAMETER_NAME + "}";

    public static final String ENGINE_TYPE = "engineType";

    /**
     * Service handling hypermedia resources
     */
    @Autowired
    private IResourceService resourceService;

    @Autowired
    private ISearchEngineConfigurationService service;

    @Autowired
    private IPluginService pluginService;

    @GetMapping
    @Operation(summary = "Get engine configurations", description = "Return a page of engine configurations")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "All engine configurations were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve all search engine configurations, matching provided engine type if provided",
                    role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<PagedModel<EntityModel<SearchEngineConfiguration>>> retrieveConfs(
        @RequestParam(value = ENGINE_TYPE, required = false) final String engineType,
        @PageableDefault(sort = "label", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) final PagedResourcesAssembler<SearchEngineConfiguration> assembler)
        throws ModuleException {

        return new ResponseEntity<>(toPagedResources(service.retrieveConfs(Optional.ofNullable(engineType), pageable),
                                                     assembler), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = CONF_ID_PATH)
    @ResourceAccess(description = "Retrieve a search engine configuration", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<SearchEngineConfiguration>> retrieveConf(
        @PathVariable(CONF_ID_PARAMETER_NAME) Long confId) throws ModuleException {
        final SearchEngineConfiguration conf = service.retrieveConf(confId);
        return new ResponseEntity<>(toResource(conf), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = CONF_ID_PATH)
    @ResourceAccess(description = "Delete a search engine configuration", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<SearchEngineConfiguration>> deleteConf(
        @PathVariable(CONF_ID_PARAMETER_NAME) final Long confId) throws ModuleException {
        service.deleteConf(confId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Create a new search engine configuration", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<SearchEngineConfiguration>> createConf(@Valid @RequestBody
                                                                             SearchEngineConfiguration conf)
        throws ModuleException {
        final SearchEngineConfiguration newConf = service.createConf(conf);
        return new ResponseEntity<>(toResource(newConf), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.PUT, value = CONF_ID_PATH)
    @ResourceAccess(description = "Update a search engine configuration", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<EntityModel<SearchEngineConfiguration>> updateConf(
        @PathVariable(CONF_ID_PARAMETER_NAME) final Long confId, @Valid @RequestBody SearchEngineConfiguration conf)
        throws ModuleException {
        if (!confId.equals(conf.getId())) {
            throw new EntityInvalidException("Search engine configuration id does not match the id to update.");
        }
        final SearchEngineConfiguration updatedConf = service.updateConf(conf);
        return new ResponseEntity<>(toResource(updatedConf), HttpStatus.OK);
    }

    @Override
    public EntityModel<SearchEngineConfiguration> toResource(SearchEngineConfiguration element, Object... extras) {
        Long id = element.getId();
        PluginConfiguration configuration = element.getConfiguration();
        String datasetUrn = element.getDatasetUrn();

        boolean addDefaultSearchLinks = true;
        List<Link> extraLinks = new ArrayList<>();

        try {
            ISearchEngine<?, ?, ?, ?> plugin = pluginService.getPlugin(configuration.getBusinessId());
            addDefaultSearchLinks = plugin.useDefaultConfigurationLinks();
            extraLinks = plugin.extraLinks(SearchEngineController.class, element);
        } catch (Exception e) {
            LOGGER.warn("Could not get extra links from plugin: {} {}",
                        configuration.getPluginId(),
                        configuration.getBusinessId(),
                        e);
        }

        final EntityModel<SearchEngineConfiguration> resource = resourceService.toResource(element);

        // Always add the config CRUD links
        addDefaultConfigurationCrudLinks(id, configuration, datasetUrn, resource);

        // Some plugins provide their own search links and do not use the default ones
        if (addDefaultSearchLinks) {
            addDefaultSearchLinksToResource(configuration, datasetUrn, resource);
        }

        // Some plugins provide specific links
        resource.add(extraLinks);

        return resource;
    }

    private void addDefaultConfigurationCrudLinks(Long id,
                                                  PluginConfiguration configuration,
                                                  String datasetUrn,
                                                  EntityModel<SearchEngineConfiguration> resource) {
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrieveConf",
                                LinkRels.SELF,
                                MethodParamFactory.build(Long.class, id));
        if ((datasetUrn != null) || !configuration.getPluginId().equals(SearchEngineMappings.LEGACY_PLUGIN_ID)) {
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "deleteConf",
                                    LinkRels.DELETE,
                                    MethodParamFactory.build(Long.class, id));
        }
        resourceService.addLink(resource,
                                this.getClass(),
                                "updateConf",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, id),
                                MethodParamFactory.build(SearchEngineConfiguration.class));
    }

    private void addDefaultSearchLinksToResource(PluginConfiguration configuration,
                                                 String datasetUrn,
                                                 EntityModel<SearchEngineConfiguration> resource) {
        if (datasetUrn == null) {
            resourceService.addLink(resource,
                                    SearchEngineController.class,
                                    "searchAll",
                                    LinkRelation.of("search"),
                                    MethodParamFactory.build(String.class, configuration.getPluginId()),
                                    MethodParamFactory.build(HttpHeaders.class),
                                    MethodParamFactory.build(MultiValueMap.class),
                                    MethodParamFactory.build(Pageable.class));
            resourceService.addLink(resource,
                                    SearchEngineController.class,
                                    "searchAllDataobjects",
                                    LinkRelation.of("search-objects"),
                                    MethodParamFactory.build(String.class, configuration.getPluginId()),
                                    MethodParamFactory.build(HttpHeaders.class),
                                    MethodParamFactory.build(MultiValueMap.class),
                                    MethodParamFactory.build(Pageable.class));
            resourceService.addLink(resource,
                                    SearchEngineController.class,
                                    "searchAllDatasets",
                                    LinkRelation.of("search-datasets"),
                                    MethodParamFactory.build(String.class, configuration.getPluginId()),
                                    MethodParamFactory.build(HttpHeaders.class),
                                    MethodParamFactory.build(MultiValueMap.class),
                                    MethodParamFactory.build(Pageable.class));
            resourceService.addLink(resource,
                                    SearchEngineController.class,
                                    "searchAllCollections",
                                    LinkRelation.of("search-collections"),
                                    MethodParamFactory.build(String.class, configuration.getPluginId()),
                                    MethodParamFactory.build(HttpHeaders.class),
                                    MethodParamFactory.build(MultiValueMap.class),
                                    MethodParamFactory.build(Pageable.class));

        } else {
            resourceService.addLink(resource,
                                    SearchEngineController.class,
                                    "searchSingleDataset",
                                    LinkRelation.of("search"),
                                    MethodParamFactory.build(String.class, configuration.getPluginId()),
                                    MethodParamFactory.build(String.class, datasetUrn),
                                    MethodParamFactory.build(HttpHeaders.class),
                                    MethodParamFactory.build(MultiValueMap.class),
                                    MethodParamFactory.build(Pageable.class));
        }
    }

}
