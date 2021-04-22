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
package fr.cnes.regards.modules.search.rest;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.gson.IAttributeHelper;
import fr.cnes.regards.modules.model.gson.helper.AttributeHelper;
import fr.cnes.regards.modules.search.domain.plugin.IEntityLinkBuilder;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.service.SearchException;
import fr.cnes.regards.modules.search.service.engine.ISearchEngineDispatcher;

/**
 * This controller manages search engines on top of system search stack<br/>
 * Each endpoint delegates search to engine service that dispatch request handling to right search engine plugin
 *
 * @author Marc Sordi
 */
@RestController
@RequestMapping(path = SearchEngineMappings.TYPE_MAPPING)
public class SearchEngineController implements IEntityLinkBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineController.class);

    // Controller method names for HATEAOAS

    private static final String SEARCH_ALL_METHOD = "searchAll";

    private static final String SEARCH_ALL_COLLECTIONS_METHOD = "searchAllCollections";

    private static final String GET_COLLECTION_METHOD = "getCollection";

    private static final String SEARCH_ALL_DATASETS_METHOD = "searchAllDatasets";

    private static final String GET_DATASET_METHOD = "getDataset";

    private static final String SEARCH_ALL_DATAOBJECTS_METHOD = "searchAllDataobjects";

    private static final String GET_DATAOBJECT_METHOD = "getDataobject";

    private static final String SEARCH_ALL_DATAOBJECTS_BY_DATASET = "searchSingleDataset";

    private static final String SEARCH_DATAOBJECTS_DATASETS = "searchDataobjectsReturnDatasets";

    private static final String EXTRA_METHOD_SUFFIX = "Extra";

    /**
     * HATEOAS link to reach dataobjects from a dataset
     */
    private static final LinkRelation LINK_TO_DATAOBJECTS = LinkRelation.of("dataobjects");

    /**
     * Pagination property
     */
    private static final String PAGE_NUMBER = "page";

    /**
     * Pagination property
     */
    private static final String PAGE_SIZE = "size";

    /**
     * Engine request dispatcher
     */
    @Autowired
    private ISearchEngineDispatcher dispatcher;

    @Autowired
    private IAttributeHelper attributeHelper;

    @Autowired
    private IResourceService resourceService;

    private static String getMethod(SearchContext context) throws UnsupportedOperationException {
        switch (context.getSearchType()) {
            case ALL:
                return SearchEngineController.SEARCH_ALL_METHOD;
            case COLLECTIONS:
                return SearchEngineController.SEARCH_ALL_COLLECTIONS_METHOD;
            case DATAOBJECTS:
                if (context.getDatasetUrn().isPresent()) {
                    // Search on single dataset
                    return SearchEngineController.SEARCH_ALL_DATAOBJECTS_BY_DATASET;
                } else {
                    return SearchEngineController.SEARCH_ALL_DATAOBJECTS_METHOD;
                }
            case DATASETS:
                return SearchEngineController.SEARCH_ALL_DATASETS_METHOD;
            case DATAOBJECTS_RETURN_DATASETS:
                return SearchEngineController.SEARCH_DATAOBJECTS_DATASETS;
            default:
                throw new UnsupportedOperationException("Unsupported search type : " + context.getSearchType());
        }
    }

    private static void addLink(List<Link> links, Link link) {
        if (link != null) {
            links.add(link);
        }
    }

    /**
     * Search on all index regardless the entity type
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_ALL_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for global search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAll(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all entities delegated to engine \"{}\"", engineType);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.ALL, engineType, headers, queryParams, pageable), this);
    }

    /**
     * Extra mapping related to search all request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_ALL_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for global search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.EXTRA) String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Extra mapping \"{}\" handling delegated to engine \"{}\"", extra, engineType);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.ALL, engineType, headers, queryParams, pageable)
                                         .withExtra(extra), this);
    }

    /**
     * Get an entity from its URN regardless its type
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_ENTITY_MAPPING)
    @ResourceAccess(description = "Generic endpoint for retrieving an entity", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> getEntity(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @Valid @PathVariable(SearchEngineMappings.URN) String urn, @RequestHeader HttpHeaders headers)
            throws ModuleException {
        LOGGER.debug("Get entity \"{}\" delegated to engine \"{}\"", urn, engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.ALL, engineType, headers, null, null)
                                                  .withUrn(UniformResourceName.fromString(urn)), this);
    }

    // Collection mappings

    /**
     * Search on all collections
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for collection search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllCollections(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all collections delegated to engine \"{}\"", engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.COLLECTIONS,
                                                              engineType,
                                                              headers,
                                                              queryParams,
                                                              pageable), this);
    }

    /**
     * Extra mapping related to search on all collections request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for collection search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllCollectionsExtra(
            @PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.EXTRA) String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search all collections extra mapping \"{}\" handling delegated to engine \"{}\"",
                     extra,
                     engineType);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.COLLECTIONS, engineType, headers, queryParams, pageable)
                                         .withExtra(extra), this);
    }

    /**
     * Search property values on all collections request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_COLLECTIONS_PROPERTY_VALUES)
    @ResourceAccess(description = "Get collection property values", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchCollectionPropertyValues(
            @PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount) throws ModuleException {
        LOGGER.debug("Search collection property values for \"{}\" delegated to engine \"{}\"",
                     propertyName,
                     engineType);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.COLLECTIONS, engineType, headers, queryParams, null)
                                         .withPropertyName(propertyName).withMaxCount(maxCount), this);
    }

    /**
     * Get a collection from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_COLLECTION_MAPPING)
    @ResourceAccess(description = "Allows to retrieve a collection", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> getCollection(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @Valid @PathVariable(SearchEngineMappings.URN) String urn, @RequestHeader HttpHeaders headers)
            throws ModuleException {
        LOGGER.debug("Get collection \"{}\" delegated to engine \"{}\"", urn, engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.COLLECTIONS, engineType, headers, null, null)
                                                  .withUrn(UniformResourceName.fromString(urn)), this);
    }

    // Dataset mappings

    /**
     * Search on all datasets
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASETS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDatasets(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all datasets delegated to engine \"{}\"", engineType);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.DATASETS, engineType, headers, queryParams, pageable),
                                 this);
    }

    /**
     * Extra mapping related to search on all datasets request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASETS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDatasetsExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.EXTRA) String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search all datasets extra mapping \"{}\" handling delegated to engine \"{}\"", extra, engineType);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.DATASETS, engineType, headers, queryParams, pageable)
                                         .withExtra(extra), this);
    }

    /**
     * Search property values on all datasets request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASETS_PROPERTY_VALUES)
    @ResourceAccess(description = "Get dataset property values", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchDatasetPropertyValues(
            @PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount) throws ModuleException {
        LOGGER.debug("Search dataset property values for \"{}\" delegated to engine \"{}\"", propertyName, engineType);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.DATASETS, engineType, headers, queryParams, null)
                                         .withPropertyName(propertyName).withMaxCount(maxCount), this);
    }

    /**
     * Get a dataset from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_DATASET_MAPPING)
    @ResourceAccess(description = "Allows to retrieve a dataset", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> getDataset(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @Valid @PathVariable(SearchEngineMappings.URN) String urn, @RequestHeader HttpHeaders headers)
            throws ModuleException {
        LOGGER.debug("Get dataset \"{}\" delegated to engine \"{}\"", urn, engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DATASETS, engineType, headers, null, null)
                                                  .withUrn(UniformResourceName.fromString(urn)), this);
    }

    // Dataobject mappings

    /**
     * Search on all dataobjects
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for dataobject search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDataobjects(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all dataobjects delegated to engine \"{}\"", engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS,
                                                              engineType,
                                                              headers,
                                                              queryParams,
                                                              pageable), this);
    }

    /**
     * Extra mapping related to search on all dataobjects request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for dataobject search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDataobjectsExtra(
            @PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.EXTRA) String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search all dataobjects extra mapping \"{}\" handling delegated to engine \"{}\"",
                     extra,
                     engineType);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS, engineType, headers, queryParams, pageable)
                                         .withExtra(extra), this);
    }

    /**
     * Search property values on all dataobjects request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTY_VALUES)
    @ResourceAccess(description = "Get dataobject property values", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchDataobjectPropertyValues(
            @PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount) throws ModuleException {
        LOGGER.debug("Search dataobject property values for \"{}\" delegated to engine \"{}\"",
                     propertyName,
                     engineType);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS, engineType, headers, queryParams, null)
                                         .withPropertyName(propertyName).withMaxCount(maxCount), this);
    }

    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTIES_BOUNDS)
    @ResourceAccess(description = "Get dataobject property values", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchDataobjectPropertiesBounds(
            @PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam(name = SearchEngineMappings.PROPERTY_NAMES) List<String> propertyNames,
            @RequestParam MultiValueMap<String, String> queryParams) throws SearchException, ModuleException {
        LOGGER.debug("Search dataobject properties bounds valuesdelegated to engine \"{}\"", engineType);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS, engineType, headers, queryParams, null)
                                         .withPropertyNames(propertyNames).withBoundCalculation(), this);
    }

    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_ATTRIBUTES)
    @ResourceAccess(description = "Get common model attributes associated to data objects results of the given request",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<Set<EntityModel<AttributeModel>>> searchDataobjectsAttributes(
            @PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams) throws SearchException, ModuleException {
        LOGGER.debug("Get dataobject model common attributes delegated to engine \"{}\"", engineType);
        ResponseEntity<List<String>> result = dispatcher
                .dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS, engineType, headers, queryParams, null)
                                         .withPropertyName(AttributeHelper.MODEL_ATTRIBUTE).withMaxCount(100), this);
        Set<AttributeModel> attrs = attributeHelper.getAllCommonAttributes(result.getBody());
        return ResponseEntity.ok(attrs.stream().map(a -> resourceService.toResource(a)).collect(Collectors.toSet()));
    }

    /**
     * Get a dataobject from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_DATAOBJECT_MAPPING)
    @ResourceAccess(description = "Allows to retrieve a dataobject", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> getDataobject(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @Valid @PathVariable(SearchEngineMappings.URN) String urn, @RequestHeader HttpHeaders headers)
            throws ModuleException {
        LOGGER.debug("Get dataobject \"{}\" delegated to engine \"{}\"", urn, engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS, engineType, headers, null, null)
                                                  .withUrn(UniformResourceName.fromString(urn)), this);
    }

    /**
     * Search dataobjects on a single dataset<br/>
     *
     * This method uses a {@link String} to handle dataset URN because HATEOAS link builder cannot manage complex type
     * conversion at the moment. It does not consider custom {@link Converter}.
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for single dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchSingleDataset(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.DATASET_URN) String datasetUrn, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search dataobjects on dataset \"{}\" delegated to engine \"{}\"",
                     datasetUrn.toString(),
                     engineType);
        UniformResourceName urn = UniformResourceName.fromString(datasetUrn);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS, engineType, headers, queryParams, pageable)
                                         .withDatasetUrn(urn), this);
    }

    /**
     * Extra mapping related to search on a single dataset request<br/>
     *
     * This method uses a {@link String} to handle dataset URN because HATEOAS link builder cannot manage complex type
     * conversion at the moment. It does not consider custom {@link Converter}.
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for single dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchSingleDatasetExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.DATASET_URN) String datasetUrn,
            @PathVariable(SearchEngineMappings.EXTRA) String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search dataobjects on dataset \"{}\" extra mapping \"{}\" handling delegated to engine \"{}\"",
                     datasetUrn,
                     extra,
                     engineType);
        UniformResourceName urn = UniformResourceName.fromString(datasetUrn);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS, engineType, headers, queryParams, pageable)
                                         .withDatasetUrn(urn).withExtra(extra), this);
    }

    /**
     * Search property values on dataobjects of a single dataset request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_PROPERTY_VALUES)
    @ResourceAccess(description = "Get dataobject property values within a dataset ", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchDataobjectPropertyValuesOnDataset(
            @PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.DATASET_URN) String datasetUrn,
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount) throws ModuleException {
        LOGGER.debug("Search dataobject property values for \"{}\" on dataset \"{}\" delegated to engine \"{}\"",
                     propertyName,
                     datasetUrn,
                     engineType);
        UniformResourceName urn = UniformResourceName.fromString(datasetUrn);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS, engineType, headers, queryParams, null)
                                         .withDatasetUrn(urn).withPropertyName(propertyName).withMaxCount(maxCount),
                                 this);
    }

    /**
     * Search dataobjects returning datasets
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for dataset search with dataobject criterions",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchDataobjectsReturnDatasets(
            @PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search datasets with dataobject criterions delegated to engine \"{}\"", engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS_RETURN_DATASETS,
                                                              engineType,
                                                              headers,
                                                              queryParams,
                                                              pageable), this);
    }

    /**
     * Build all pagination links from the ginve page results.
     * Can generate previous, current and next page links.
     * @param resourceService
     * @param page
     * @param context
     * @return {@link Link}s
     */
    @Override
    public List<Link> buildPaginationLinks(IResourceService resourceService, PageImpl<?> page, SearchContext context) {
        List<Link> links = new ArrayList<>();
        // Build previous link
        if (page.hasPrevious()) {
            int pageNumber = context.getPageable().getPageNumber() - 1;
            links.add(buildPaginationLink(resourceService,
                                          context,
                                          page.getSize(),
                                          pageNumber,
                                          IanaLinkRelations.PREV));
        }
        // Build current page link
        links.add(buildPaginationLink(resourceService,
                                      context,
                                      page.getSize(),
                                      page.getNumber(),
                                      IanaLinkRelations.SELF));
        // Build next link
        if (page.hasNext()) {
            int pageNumber = context.getPageable().getPageNumber() - 1;
            links.add(buildPaginationLink(resourceService,
                                          context,
                                          page.getSize(),
                                          pageNumber,
                                          IanaLinkRelations.NEXT));
        }
        return links;
    }

    /**
     *
     * Return a contextual link
     * @return {@link Link}, may be null.
     */
    @Override
    public Link buildPaginationLink(IResourceService resourceService, SearchContext context, int pageSize,
            int pageNumber, LinkRelation rel) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.putAll(context.getQueryParams());
        SearchContext newContext = SearchContext.build(context.getSearchType(),
                                                       context.getEngineType(),
                                                       context.getHeaders(),
                                                       params,
                                                       context.getPageable());
        newContext.getQueryParams().put(PAGE_NUMBER, Arrays.asList(String.valueOf(pageNumber)));
        newContext.getQueryParams().put(PAGE_SIZE, Arrays.asList(String.valueOf(pageSize)));
        return buildPaginationLink(resourceService, newContext, rel);
    }

    /**
     * Return a contextual link
     * @return {@link Link}, may be null.
     */
    @Override
    public Link buildPaginationLink(IResourceService resourceService, SearchContext context, LinkRelation rel) {
        if (context.getDatasetUrn().isPresent()) {
            return resourceService.buildLinkWithParams(SearchEngineController.class,
                                                       getMethod(context),
                                                       rel,
                                                       MethodParamFactory.build(String.class, context.getEngineType()),
                                                       MethodParamFactory.build(String.class,
                                                                                context.getDatasetUrn().get()
                                                                                        .toString()),
                                                       MethodParamFactory.build(HttpHeaders.class),
                                                       MethodParamFactory
                                                               .build(MultiValueMap.class, context.getQueryParams()),
                                                       MethodParamFactory.build(Pageable.class));
        } else {
            return resourceService.buildLinkWithParams(SearchEngineController.class,
                                                       getMethod(context),
                                                       rel,
                                                       MethodParamFactory.build(String.class, context.getEngineType()),
                                                       MethodParamFactory.build(HttpHeaders.class),
                                                       MethodParamFactory
                                                               .build(MultiValueMap.class, context.getQueryParams()),
                                                       MethodParamFactory.build(Pageable.class));
        }
    }

    /**
     * Return a contextual link
     * @return {@link Link}, may be null.
     */
    @Override
    public Link buildExtraLink(IResourceService resourceService, SearchContext context, LinkRelation rel,
            String extra) {
        if (context.getDatasetUrn().isPresent()) {
            return resourceService.buildLinkWithParams(SearchEngineController.class,
                                                       getMethod(context) + EXTRA_METHOD_SUFFIX,
                                                       rel,
                                                       MethodParamFactory.build(String.class, context.getEngineType()),
                                                       MethodParamFactory.build(String.class,
                                                                                context.getDatasetUrn().get()
                                                                                        .toString()),
                                                       MethodParamFactory.build(String.class, extra),
                                                       MethodParamFactory.build(HttpHeaders.class),
                                                       MethodParamFactory
                                                               .build(MultiValueMap.class, context.getQueryParams()),
                                                       MethodParamFactory.build(Pageable.class));
        } else {
            return resourceService.buildLinkWithParams(SearchEngineController.class,
                                                       getMethod(context) + EXTRA_METHOD_SUFFIX,
                                                       rel,
                                                       MethodParamFactory.build(String.class, context.getEngineType()),
                                                       MethodParamFactory.build(String.class, extra),
                                                       MethodParamFactory.build(HttpHeaders.class),
                                                       MethodParamFactory
                                                               .build(MultiValueMap.class, context.getQueryParams()),
                                                       MethodParamFactory.build(Pageable.class));
        }
    }

    /**
     * Build contextual entity links according to search context and entity type
     */
    @Override
    public List<Link> buildEntityLinks(IResourceService resourceService, SearchContext context, EntityFeature entity) {
        if (entity != null) {
            return buildEntityLinks(resourceService, context, entity.getEntityType(), entity.getId());
        } else {
            return Lists.newArrayList();
        }
    }

    /**
     * Build contextual entity links according to search context and entity type
     */
    @Override
    public List<Link> buildEntityLinks(IResourceService resourceService, SearchContext context, EntityType entityType,
            UniformResourceName id) {
        List<Link> links = new ArrayList<>();

        String idString = id.toString();
        switch (entityType) {
            case COLLECTION:
                addLink(links,
                        resourceService.buildLink(SearchEngineController.class,
                                                  SearchEngineController.GET_COLLECTION_METHOD,
                                                  LinkRels.SELF,
                                                  MethodParamFactory.build(String.class, context.getEngineType()),
                                                  MethodParamFactory.build(String.class, idString),
                                                  MethodParamFactory.build(HttpHeaders.class)));
                break;
            case DATA:
                addLink(links,
                        resourceService.buildLink(SearchEngineController.class,
                                                  SearchEngineController.GET_DATAOBJECT_METHOD,
                                                  LinkRels.SELF,
                                                  MethodParamFactory.build(String.class, context.getEngineType()),
                                                  MethodParamFactory.build(String.class, idString),
                                                  MethodParamFactory.build(HttpHeaders.class)));
                break;
            case DATASET:
                addLink(links,
                        resourceService.buildLink(SearchEngineController.class,
                                                  SearchEngineController.GET_DATASET_METHOD,
                                                  LinkRels.SELF,
                                                  MethodParamFactory.build(String.class, context.getEngineType()),
                                                  MethodParamFactory.build(String.class, idString),
                                                  MethodParamFactory.build(HttpHeaders.class)));
                // Add link to DATA OBJECTS
                addLink(links,
                        resourceService.buildLink(SearchEngineController.class,
                                                  SearchEngineController.SEARCH_ALL_DATAOBJECTS_BY_DATASET,
                                                  LINK_TO_DATAOBJECTS,
                                                  MethodParamFactory.build(String.class, context.getEngineType()),
                                                  MethodParamFactory.build(String.class, idString),
                                                  MethodParamFactory.build(HttpHeaders.class),
                                                  MethodParamFactory.build(MultiValueMap.class),
                                                  MethodParamFactory.build(Pageable.class)));
                break;

            default:
                // Nothing to do
                LOGGER.warn("Unknown entity type \"{}\"", entityType);
                break;
        }
        return links;
    }
}
