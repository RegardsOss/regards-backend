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
package fr.cnes.regards.modules.search.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map.Entry;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
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

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.domain.plugin.SearchType;
import fr.cnes.regards.modules.search.rest.engine.ISearchEngineDispatcher;
import fr.cnes.regards.modules.search.service.SearchException;

/**
 * This controller manages search engines on top of system search stack<br/>
 * Each endpoint delegates search to engine service that dispatch request handling to right search engine plugin
 *
 * @author Marc Sordi
 */
@RestController
@RequestMapping(path = SearchEngineController.TYPE_MAPPING)
public class SearchEngineController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineController.class);

    /**
     * Search main namespace
     */
    public static final String TYPE_MAPPING = "/engines/{engineType}";

    /**
     * Search route mapping
     */
    public static final String SEARCH_MAPPING = "/search";

    /**
     * Additional route mapping
     */
    public static final String EXTRA_MAPPING = "/{extra}";

    /**
     * To retrieve a single entity
     */
    public static final String URN_MAPPING = "/{urn}";

    /**
     * For entities with description
     */
    public static final String DESCRIPTION_MAPPING = "/description";

    // Search on all entities

    public static final String ENTITIES_MAPPING = "/entities";

    public static final String SEARCH_ALL_MAPPING = ENTITIES_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_ALL_MAPPING_EXTRA = SEARCH_ALL_MAPPING + EXTRA_MAPPING;

    public static final String GET_ENTITY_MAPPING = ENTITIES_MAPPING + URN_MAPPING;

    // Collection mappings

    public static final String COLLECTIONS_MAPPING = "/collections";

    public static final String SEARCH_COLLECTIONS_MAPPING = COLLECTIONS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_COLLECTIONS_MAPPING_EXTRA = SEARCH_COLLECTIONS_MAPPING + EXTRA_MAPPING;

    public static final String GET_COLLECTION_MAPPING = COLLECTIONS_MAPPING + URN_MAPPING;

    // Document mappings

    public static final String DOCUMENTS_MAPPING = "/documents";

    public static final String SEARCH_DOCUMENTS_MAPPING = DOCUMENTS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_DOCUMENTS_MAPPING_EXTRA = SEARCH_DOCUMENTS_MAPPING + EXTRA_MAPPING;

    public static final String GET_DOCUMENT_MAPPING = DOCUMENTS_MAPPING + URN_MAPPING;

    // Dataset mapping

    public static final String DATASETS_MAPPING = "/datasets";

    public static final String SEARCH_DATASETS_MAPPING = DATASETS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_DATASETS_MAPPING_EXTRA = SEARCH_DATASETS_MAPPING + EXTRA_MAPPING;

    public static final String GET_DATASET_MAPPING = DATASETS_MAPPING + URN_MAPPING;

    public static final String GET_DATASET_DESCRIPTION_MAPPING = GET_DATASET_MAPPING + DESCRIPTION_MAPPING;

    // Dataobject mapping

    public static final String DATAOBJECTS_MAPPING = "/dataobjects";

    public static final String SEARCH_DATAOBJECTS_MAPPING = DATAOBJECTS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_DATAOBJECTS_MAPPING_EXTRA = SEARCH_DATAOBJECTS_MAPPING + EXTRA_MAPPING;

    public static final String GET_DATAOBJECT_MAPPING = DATAOBJECTS_MAPPING + URN_MAPPING;

    // Search dataobjects on a single dataset mapping

    public static final String DATASET_DATAOBJECTS_MAPPING = "/datasets/{datasetId}/dataobjects";

    public static final String SEARCH_DATASET_DATAOBJECTS_MAPPING = DATASET_DATAOBJECTS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA = SEARCH_DATASET_DATAOBJECTS_MAPPING
            + EXTRA_MAPPING;

    public static final String GET_DATASET_DATAOBJECT_MAPPING = DATASET_DATAOBJECTS_MAPPING + URN_MAPPING;

    // Search on dataobjects returning datasets

    public static final String SEARCH_DATAOBJECTS_DATASETS_MAPPING = "/dataobjects/datasets" + SEARCH_MAPPING;

    // Controller method names for HATEAOAS

    public static final String SEARCH_ALL_METHOD = "searchAll";

    public static final String SEARCH_ALL_COLLECTIONS_METHOD = "searchAllCollections";

    public static final String SEARCH_ALL_DOCUMENTS_METHOD = "searchAllDocuments";

    public static final String SEARCH_ALL_DATASETS_METHOD = "searchAllDatasets";

    public static final String SEARCH_ALL_DATAOBJECTS_METHOD = "searchAllDataobjects";

    public static final String SEARCH_ALL_DATAOBJECTS_BY_DATASET = "searchSingleDataset";

    public static final String SEARCH_DATAOBJECTS_DATASETS = "searchDataobjectsReturnDatasets";

    @Autowired
    private ISearchEngineDispatcher dispatcher;

    // Search on all entities

    /**
     * Search on all index regardless the entity type
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_ALL_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for global search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAll(@PathVariable String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all entities delegated to engine \"{}\"", engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.ALL, engineType, headers,
                                                              getDecodedParams(queryParams), pageable));
    }

    /**
     * Extra mapping related to search all request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_ALL_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for global search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllExtra(@PathVariable String engineType, @PathVariable String extra,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Extra mapping \"{}\" handling delegated to engine \"{}\"", extra, engineType);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.ALL, engineType, headers, getDecodedParams(queryParams), pageable).withExtra(extra));
    }

    /**
     * Get an entity from its URN regardless its type
     */
    @RequestMapping(method = RequestMethod.GET, value = GET_ENTITY_MAPPING)
    @ResourceAccess(description = "Generic endpoint for retrieving an entity", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> getEntity(@PathVariable String engineType, @Valid @PathVariable UniformResourceName urn,
            @RequestHeader HttpHeaders headers) throws ModuleException {
        LOGGER.debug("Get entity \"{}\" delegated to engine \"{}\"", urn.toString(), engineType);
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.ALL, engineType, headers, null, null).withUrn(urn));
    }

    // Collection mappings

    /**
     * Search on all collections
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_COLLECTIONS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for collection search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllCollections(@PathVariable String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all collections delegated to engine \"{}\"", engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.COLLECTIONS, engineType, headers,
                                                              getDecodedParams(queryParams), pageable));
    }

    /**
     * Extra mapping related to search on all collections request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_COLLECTIONS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for collection search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllCollectionsExtra(@PathVariable String engineType, @PathVariable String extra,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search all collections extra mapping \"{}\" handling delegated to engine \"{}\"", extra,
                     engineType);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.COLLECTIONS, engineType, headers, getDecodedParams(queryParams), pageable)
                .withExtra(extra));
    }

    /**
     * Get a collection from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = GET_COLLECTION_MAPPING)
    @ResourceAccess(description = "Allows to retrieve a collection", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> getCollection(@PathVariable String engineType,
            @Valid @PathVariable UniformResourceName urn, @RequestHeader HttpHeaders headers) throws ModuleException {
        LOGGER.debug("Get collection \"{}\" delegated to engine \"{}\"", urn.toString(), engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.COLLECTIONS, engineType, headers, null, null)
                .withUrn(urn));
    }

    // Document mappings

    /**
     * Search on all documents
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DOCUMENTS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for document search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDocuments(@PathVariable String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all documents delegated to engine \"{}\"", engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DOCUMENTS, engineType, headers,
                                                              getDecodedParams(queryParams), pageable));
    }

    /**
     * Extra mapping related to search on all documents request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DOCUMENTS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for document search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDocumentsExtra(@PathVariable String engineType, @PathVariable String extra,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search all documents extra mapping \"{}\" handling delegated to engine \"{}\"", extra,
                     engineType);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.DOCUMENTS, engineType, headers, getDecodedParams(queryParams), pageable)
                .withExtra(extra));
    }

    /**
     * Get a document from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = GET_DOCUMENT_MAPPING)
    @ResourceAccess(description = "Allows to retrieve a document", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> getDocument(@PathVariable String engineType, @Valid @PathVariable UniformResourceName urn,
            @RequestHeader HttpHeaders headers) throws ModuleException {
        LOGGER.debug("Get document \"{}\" delegated to engine \"{}\"", urn.toString(), engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DOCUMENTS, engineType, headers, null, null)
                .withUrn(urn));
    }

    // Dataset mappings

    /**
     * Search on all datasets
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATASETS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDatasets(@PathVariable String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all datasets delegated to engine \"{}\"", engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DATASETS, engineType, headers,
                                                              getDecodedParams(queryParams), pageable));
    }

    /**
     * Extra mapping related to search on all datasets request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATASETS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDatasetsExtra(@PathVariable String engineType, @PathVariable String extra,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search all datasets extra mapping \"{}\" handling delegated to engine \"{}\"", extra, engineType);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.DATASETS, engineType, headers, getDecodedParams(queryParams), pageable)
                .withExtra(extra));
    }

    /**
     * Get a dataset from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = GET_DATASET_MAPPING)
    @ResourceAccess(description = "Allows to retrieve a dataset", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> getDataset(@PathVariable String engineType, @Valid @PathVariable UniformResourceName urn,
            @RequestHeader HttpHeaders headers) throws ModuleException {
        LOGGER.debug("Get dataset \"{}\" delegated to engine \"{}\"", urn.toString(), engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DATASETS, engineType, headers, null, null)
                .withUrn(urn));
    }

    /**
     * Get a dataset description from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = GET_DATASET_DESCRIPTION_MAPPING)
    @ResourceAccess(description = "Allows to retrieve a dataset", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> getDatasetDescription(@PathVariable String engineType,
            @Valid @PathVariable UniformResourceName urn, @RequestHeader HttpHeaders headers) throws ModuleException {
        LOGGER.debug("Get dataset \"{}\" delegated to engine \"{}\"", urn.toString(), engineType);
        // FIXME how to manage this endpoint : plugin or not?
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DATASETS, engineType, headers, null, null)
                .withUrn(urn));
    }

    // Dataobject mappings

    /**
     * Search on all dataobjects
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATAOBJECTS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for dataobject search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDataobjects(@PathVariable String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all dataobjects delegated to engine \"{}\"", engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS, engineType, headers,
                                                              getDecodedParams(queryParams), pageable));
    }

    /**
     * Extra mapping related to search on all dataobjects request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATAOBJECTS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for dataobject search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDataobjectsExtra(@PathVariable String engineType, @PathVariable String extra,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search all dataobjects extra mapping \"{}\" handling delegated to engine \"{}\"", extra,
                     engineType);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.DATAOBJECTS, engineType, headers, getDecodedParams(queryParams), pageable)
                .withExtra(extra));
    }

    /**
     * Get a dataobject from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = GET_DATAOBJECT_MAPPING)
    @ResourceAccess(description = "Allows to retrieve a dataobject", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> getDataobject(@PathVariable String engineType,
            @Valid @PathVariable UniformResourceName urn, @RequestHeader HttpHeaders headers) throws ModuleException {
        LOGGER.debug("Get dataobject \"{}\" delegated to engine \"{}\"", urn.toString(), engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS, engineType, headers, null, null)
                .withUrn(urn));
    }

    // Search dataobjects on a single dataset mapping

    /**
     * Search dataobjects on a single dataset
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATASET_DATAOBJECTS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for single dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchSingleDataset(@PathVariable String engineType, @PathVariable String datasetId,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search dataobjects on dataset \"{}\" delegated to engine \"{}\"", datasetId, engineType);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.DATAOBJECTS, engineType, headers, getDecodedParams(queryParams), pageable)
                .withDatasetId(datasetId));
    }

    /**
     * Extra mapping related to search on a single dataset request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for single dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchSingleDatasetExtra(@PathVariable String engineType, @PathVariable String datasetId,
            @PathVariable String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search dataobjects on dataset \"{}\" extra mapping \"{}\" handling delegated to engine \"{}\"",
                     datasetId, extra, engineType);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.DATAOBJECTS, engineType, headers, getDecodedParams(queryParams), pageable)
                .withDatasetId(datasetId).withExtra(extra));
    }

    /**
     * Get a dataobject from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = GET_DATASET_DATAOBJECT_MAPPING)
    @ResourceAccess(description = "Allows to retrieve a dataobject", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> getDataobjectFromDataset(@PathVariable String engineType, @PathVariable String datasetId,
            @Valid @PathVariable UniformResourceName urn, @RequestHeader HttpHeaders headers) throws ModuleException {
        LOGGER.debug("Get dataobject \"{}\" delegated to engine \"{}\"", urn.toString(), engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS, engineType, headers, null, null)
                .withDatasetId(datasetId).withUrn(urn));
    }

    // Search on dataobjects returning datasets

    /**
     * Search dataobjects returning datasets
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATAOBJECTS_DATASETS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for dataset search with dataobject criterions",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchDataobjectsReturnDatasets(@PathVariable String engineType,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search datasets with dataobject criterions delegated to engine \"{}\"", engineType);
        return dispatcher.dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS_RETURN_DATASETS, engineType,
                                                              headers, getDecodedParams(queryParams), pageable));
    }

    // FIXME : fix issue in frontend to avoid double decoding
    private MultiValueMap<String, String> getDecodedParams(MultiValueMap<String, String> queryParams)
            throws SearchException {

        MultiValueMap<String, String> allDecodedParams = new LinkedMultiValueMap<>();
        for (Entry<String, List<String>> kvp : queryParams.entrySet()) {
            for (String value : kvp.getValue()) {
                try {
                    allDecodedParams.add(kvp.getKey(), URLDecoder.decode(value, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    String message = String.format("Unsupported query parameters \"%s\" with value \"%s\"",
                                                   kvp.getKey(), value);
                    throw new SearchException(message, e);
                }
            }
        }
        return allDecodedParams;
    }

    /**
     * Return a contextual link
     * @return {@link Link}, may be null.
     */
    public static Link buildPaginationLink(IResourceService resourceService, SearchContext context, String rel) {
        Link link;

        switch (context.getSearchType()) {
            case ALL:
                link = resourceService
                        .buildLinkWithParams(SearchEngineController.class, SearchEngineController.SEARCH_ALL_METHOD,
                                             rel, MethodParamFactory.build(String.class, context.getEngineType()),
                                             MethodParamFactory.build(HttpHeaders.class),
                                             MethodParamFactory.build(MultiValueMap.class, context.getQueryParams()),
                                             MethodParamFactory.build(Pageable.class));
                break;

            case COLLECTIONS:
                link = resourceService.buildLinkWithParams(SearchEngineController.class,
                                                           SearchEngineController.SEARCH_ALL_COLLECTIONS_METHOD, rel,
                                                           MethodParamFactory.build(String.class,
                                                                                    context.getEngineType()),
                                                           MethodParamFactory.build(HttpHeaders.class),
                                                           MethodParamFactory.build(MultiValueMap.class,
                                                                                    context.getQueryParams()),
                                                           MethodParamFactory.build(Pageable.class));
                break;
            case DATAOBJECTS:
                if (context.getDatasetId().isPresent()) {
                    // Search on single dataset
                    link = resourceService
                            .buildLinkWithParams(SearchEngineController.class,
                                                 SearchEngineController.SEARCH_ALL_DATAOBJECTS_BY_DATASET, rel,
                                                 MethodParamFactory.build(String.class, context.getEngineType()),
                                                 MethodParamFactory.build(String.class, context.getDatasetId().get()),
                                                 MethodParamFactory.build(HttpHeaders.class),
                                                 MethodParamFactory.build(MultiValueMap.class,
                                                                          context.getQueryParams()),
                                                 MethodParamFactory.build(Pageable.class));
                } else {

                    link = resourceService
                            .buildLinkWithParams(SearchEngineController.class,
                                                 SearchEngineController.SEARCH_ALL_DATAOBJECTS_METHOD, rel,
                                                 MethodParamFactory.build(String.class, context.getEngineType()),
                                                 MethodParamFactory.build(HttpHeaders.class),
                                                 MethodParamFactory.build(MultiValueMap.class,
                                                                          context.getQueryParams()),
                                                 MethodParamFactory.build(Pageable.class));
                }
                break;
            case DATASETS:
                link = resourceService.buildLinkWithParams(SearchEngineController.class,
                                                           SearchEngineController.SEARCH_ALL_DATASETS_METHOD, rel,
                                                           MethodParamFactory.build(String.class,
                                                                                    context.getEngineType()),
                                                           MethodParamFactory.build(HttpHeaders.class),
                                                           MethodParamFactory.build(MultiValueMap.class,
                                                                                    context.getQueryParams()),
                                                           MethodParamFactory.build(Pageable.class));
                break;
            case DOCUMENTS:
                link = resourceService.buildLinkWithParams(SearchEngineController.class,
                                                           SearchEngineController.SEARCH_ALL_DOCUMENTS_METHOD, rel,
                                                           MethodParamFactory.build(String.class,
                                                                                    context.getEngineType()),
                                                           MethodParamFactory.build(HttpHeaders.class),
                                                           MethodParamFactory.build(MultiValueMap.class,
                                                                                    context.getQueryParams()),
                                                           MethodParamFactory.build(Pageable.class));
                break;

            case DATAOBJECTS_RETURN_DATASETS:
                link = resourceService.buildLinkWithParams(SearchEngineController.class,
                                                           SearchEngineController.SEARCH_DATAOBJECTS_DATASETS, rel,
                                                           MethodParamFactory.build(String.class,
                                                                                    context.getEngineType()),
                                                           MethodParamFactory.build(HttpHeaders.class),
                                                           MethodParamFactory.build(MultiValueMap.class,
                                                                                    context.getQueryParams()),
                                                           MethodParamFactory.build(Pageable.class));
                break;
            default:
                throw new UnsupportedOperationException("Unsupported search type : " + context.getSearchType());
        }

        return link;
    }

}
