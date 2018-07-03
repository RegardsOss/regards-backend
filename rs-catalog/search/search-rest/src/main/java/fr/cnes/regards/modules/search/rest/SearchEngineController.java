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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.PageImpl;
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
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.entities.domain.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
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

    public static final String ENGINE_TYPE_PARAMETER = "{engineType}";

    /**
     * Search main namespace
     */
    public static final String TYPE_MAPPING = "/engines/" + ENGINE_TYPE_PARAMETER;

    /**
     * Search route mapping
     */
    private static final String SEARCH_MAPPING = "/search";

    /**
     * Additional route mapping
     */
    private static final String EXTRA_MAPPING = "/{extra}";

    /**
     * To retrieve a single entity
     */
    private static final String URN_MAPPING = "/{urn}";

    /**
     * For entities with description
     */
    private static final String DESCRIPTION_MAPPING = "/description";

    /**
     * To get all values of a property
     */
    private static final String PROPERTY_VALUES_MAPPING = "/properties/{propertyName}/values";

    /**
     * To get a file summary on a given request, only available for dataobject queries
     */
    private static final String FILE_SUMMARY_MAPPING = "/summary";

    // Search on all entities

    private static final String ENTITIES_MAPPING = "/entities";

    public static final String SEARCH_ALL_MAPPING = ENTITIES_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_ALL_MAPPING_EXTRA = SEARCH_ALL_MAPPING + EXTRA_MAPPING;

    public static final String GET_ENTITY_MAPPING = ENTITIES_MAPPING + URN_MAPPING;

    // Collection mappings

    private static final String COLLECTIONS_MAPPING = "/collections";

    public static final String SEARCH_COLLECTIONS_MAPPING = COLLECTIONS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_COLLECTIONS_MAPPING_EXTRA = SEARCH_COLLECTIONS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_COLLECTIONS_PROPERTY_VALUES = SEARCH_COLLECTIONS_MAPPING
            + PROPERTY_VALUES_MAPPING;

    public static final String GET_COLLECTION_MAPPING = COLLECTIONS_MAPPING + URN_MAPPING;

    // Document mappings

    private static final String DOCUMENTS_MAPPING = "/documents";

    public static final String SEARCH_DOCUMENTS_MAPPING = DOCUMENTS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_DOCUMENTS_MAPPING_EXTRA = SEARCH_DOCUMENTS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_DOCUMENTS_PROPERTY_VALUES = SEARCH_DOCUMENTS_MAPPING + PROPERTY_VALUES_MAPPING;

    public static final String GET_DOCUMENT_MAPPING = DOCUMENTS_MAPPING + URN_MAPPING;

    // Dataset mapping

    private static final String DATASETS_MAPPING = "/datasets";

    public static final String SEARCH_DATASETS_MAPPING = DATASETS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_DATASETS_MAPPING_EXTRA = SEARCH_DATASETS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_DATASETS_PROPERTY_VALUES = SEARCH_DATASETS_MAPPING + PROPERTY_VALUES_MAPPING;

    public static final String GET_DATASET_MAPPING = DATASETS_MAPPING + URN_MAPPING;

    public static final String GET_DATASET_DESCRIPTION_MAPPING = GET_DATASET_MAPPING + DESCRIPTION_MAPPING;

    // Dataobject mapping

    private static final String DATAOBJECTS_MAPPING = "/dataobjects";

    public static final String SEARCH_DATAOBJECTS_MAPPING = DATAOBJECTS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_DATAOBJECTS_MAPPING_EXTRA = SEARCH_DATAOBJECTS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_DATAOBJECTS_PROPERTY_VALUES = SEARCH_DATAOBJECTS_MAPPING
            + PROPERTY_VALUES_MAPPING;

    public static final String GET_DATAOBJECT_MAPPING = DATAOBJECTS_MAPPING + URN_MAPPING;

    public static final String DATAOBJECTS_SUMMARY_MAPPING = DATAOBJECTS_MAPPING + FILE_SUMMARY_MAPPING;

    // Search dataobjects on a single dataset mapping

    private static final String DATASET_DATAOBJECTS_MAPPING = "/datasets/{datasetUrn}/dataobjects";

    public static final String SEARCH_DATASET_DATAOBJECTS_MAPPING = DATASET_DATAOBJECTS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA = SEARCH_DATASET_DATAOBJECTS_MAPPING
            + EXTRA_MAPPING;

    public static final String SEARCH_DATASET_DATAOBJECTS_PROPERTY_VALUES = SEARCH_DATASET_DATAOBJECTS_MAPPING
            + PROPERTY_VALUES_MAPPING;

    public static final String DATASET_DATAOBJECTS_SUMMARY_MAPPING = DATASET_DATAOBJECTS_MAPPING + FILE_SUMMARY_MAPPING;

    // Fallback to {@link #GET_DATAOBJECT_MAPPING} for single data object retrieval

    // Search on dataobjects returning datasets

    public static final String SEARCH_DATAOBJECTS_DATASETS_MAPPING = "/dataobjects/datasets" + SEARCH_MAPPING;

    // Controller method names for HATEAOAS

    private static final String SEARCH_ALL_METHOD = "searchAll";

    private static final String SEARCH_ALL_COLLECTIONS_METHOD = "searchAllCollections";

    private static final String GET_COLLECTION_METHOD = "getCollection";

    private static final String SEARCH_ALL_DOCUMENTS_METHOD = "searchAllDocuments";

    private static final String GET_DOCUMENT_METHOD = "getDocument";

    private static final String SEARCH_ALL_DATASETS_METHOD = "searchAllDatasets";

    private static final String GET_DATASET_METHOD = "getDataset";

    private static final String GET_DATASET_DESCRIPTION_METHOD = "getDatasetDescription";

    private static final String SEARCH_ALL_DATAOBJECTS_METHOD = "searchAllDataobjects";

    private static final String GET_DATAOBJECT_METHOD = "getDataobject";

    private static final String SEARCH_ALL_DATAOBJECTS_BY_DATASET = "searchSingleDataset";

    private static final String SEARCH_DATAOBJECTS_DATASETS = "searchDataobjectsReturnDatasets";

    private static final String EXTRA_METHOD_SUFFIX = "Extra";

    /**
     * HATEOAS link to reach dataobjects from a dataset
     */
    private static final String LINK_TO_DATAOBJECTS = "dataobjects";

    /**
     * HATEOAS link to reach dataset description
     */
    private static final String LINK_TO_DATASET_DESCRIPTION = "description";

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
     * Search property values on all collections request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_COLLECTIONS_PROPERTY_VALUES)
    @ResourceAccess(description = "Get collection property values", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchCollectionPropertyValues(@PathVariable String engineType,
            @PathVariable String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam int maxCount)
            throws ModuleException {
        LOGGER.debug("Search collection property values for \"{}\" delegated to engine \"{}\"", propertyName,
                     engineType);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.COLLECTIONS, engineType, headers, getDecodedParams(queryParams), null)
                .withPropertyName(propertyName).withMaxCount(maxCount));
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
     * Search property values on all documents request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DOCUMENTS_PROPERTY_VALUES)
    @ResourceAccess(description = "Get document property values", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchDocumentPropertyValues(@PathVariable String engineType,
            @PathVariable String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam int maxCount)
            throws ModuleException {
        LOGGER.debug("Search document property values for \"{}\" delegated to engine \"{}\"", propertyName, engineType);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.DOCUMENTS, engineType, headers, getDecodedParams(queryParams), null)
                .withPropertyName(propertyName).withMaxCount(maxCount));
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
     * Search property values on all datasets request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATASETS_PROPERTY_VALUES)
    @ResourceAccess(description = "Get dataset property values", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchDatasetPropertyValues(@PathVariable String engineType,
            @PathVariable String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam int maxCount)
            throws ModuleException {
        LOGGER.debug("Search dataset property values for \"{}\" delegated to engine \"{}\"", propertyName, engineType);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.DATASETS, engineType, headers, getDecodedParams(queryParams), null)
                .withPropertyName(propertyName).withMaxCount(maxCount));
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
     * Search property values on all dataobjects request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATAOBJECTS_PROPERTY_VALUES)
    @ResourceAccess(description = "Get dataobject property values", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchDataobjectPropertyValues(@PathVariable String engineType,
            @PathVariable String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam int maxCount)
            throws ModuleException {
        LOGGER.debug("Search dataobject property values for \"{}\" delegated to engine \"{}\"", propertyName,
                     engineType);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.DATAOBJECTS, engineType, headers, getDecodedParams(queryParams), null)
                .withPropertyName(propertyName).withMaxCount(maxCount));
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

    /**
     * Compute a DocFileSummary for current user, for specified request context, for asked file types (see
     * {@link DataType})
     */
    @RequestMapping(method = RequestMethod.GET, value = DATAOBJECTS_SUMMARY_MAPPING)
    @ResourceAccess(description = "Compute dataset(s) summary", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<DocFilesSummary> computeDatasetsSummary(@PathVariable String engineType,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam String[] fileTypes) throws ModuleException {
        LOGGER.debug("Get dataobject summary delegated to engine \"{}\"", engineType);
        List<DataType> dataTypes = new ArrayList<>();
        if (fileTypes != null) {
            for (String fileType : fileTypes) {
                dataTypes.add(Enum.valueOf(DataType.class, fileType));
            }
        }
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.DATAOBJECTS, engineType, headers, queryParams, null).withDataTypes(dataTypes));
    }

    // Search dataobjects on a single dataset mapping

    /**
     * Search dataobjects on a single dataset<br/>
     *
     * This method uses a {@link String} to handle dataset URN because HATEOAS link builder cannot manage complex type
     * conversion at the moment. It does not consider custom {@link Converter}.
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATASET_DATAOBJECTS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for single dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchSingleDataset(@PathVariable String engineType, @PathVariable String datasetUrn,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search dataobjects on dataset \"{}\" delegated to engine \"{}\"", datasetUrn.toString(),
                     engineType);
        UniformResourceName urn = UniformResourceName.fromString(datasetUrn);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.DATAOBJECTS, engineType, headers, getDecodedParams(queryParams), pageable)
                .withDatasetUrn(urn));
    }

    /**
     * Extra mapping related to search on a single dataset request<br/>
     *
     * This method uses a {@link String} to handle dataset URN because HATEOAS link builder cannot manage complex type
     * conversion at the moment. It does not consider custom {@link Converter}.
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for single dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchSingleDatasetExtra(@PathVariable String engineType, @PathVariable String datasetUrn,
            @PathVariable String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search dataobjects on dataset \"{}\" extra mapping \"{}\" handling delegated to engine \"{}\"",
                     datasetUrn, extra, engineType);
        UniformResourceName urn = UniformResourceName.fromString(datasetUrn);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.DATAOBJECTS, engineType, headers, getDecodedParams(queryParams), pageable)
                .withDatasetUrn(urn).withExtra(extra));
    }

    /**
     * Search property values on dataobjects of a single dataset request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATASET_DATAOBJECTS_PROPERTY_VALUES)
    @ResourceAccess(description = "Get dataobject property values within a dataset ", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchDataobjectPropertyValuesOnDataset(@PathVariable String engineType,
            @PathVariable String datasetUrn, @PathVariable String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam int maxCount)
            throws ModuleException {
        LOGGER.debug("Search dataobject property values for \"{}\" on dataset \"{}\" delegated to engine \"{}\"",
                     propertyName, datasetUrn, engineType);
        UniformResourceName urn = UniformResourceName.fromString(datasetUrn);
        return dispatcher.dispatchRequest(SearchContext
                .build(SearchType.DATAOBJECTS, engineType, headers, getDecodedParams(queryParams), null)
                .withDatasetUrn(urn).withPropertyName(propertyName).withMaxCount(maxCount));
    }

    /**
     * Compute a DocFileSummary for current user, for specified request context, for asked file types (see
     * {@link DataType}) on a single dataset
     */
    @RequestMapping(method = RequestMethod.GET, value = DATASET_DATAOBJECTS_SUMMARY_MAPPING)
    @ResourceAccess(description = "Compute single dataset summary", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<DocFilesSummary> computeDatasetsSummary(@PathVariable String engineType,
            @PathVariable String datasetUrn, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam String[] fileTypes)
            throws ModuleException {
        LOGGER.debug("Get dataobject summary for dataset \"{}\" delegated to engine \"{}\"", datasetUrn, engineType);
        UniformResourceName urn = UniformResourceName.fromString(datasetUrn);
        List<DataType> dataTypes = new ArrayList<>();
        if (fileTypes != null) {
            for (String fileType : fileTypes) {
                dataTypes.add(Enum.valueOf(DataType.class, fileType));
            }
        }
        return dispatcher
                .dispatchRequest(SearchContext.build(SearchType.DATAOBJECTS, engineType, headers, queryParams, null)
                        .withDatasetUrn(urn).withDataTypes(dataTypes));
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

    /**
     * Build all pagination links from the ginve page results.
     * Can generate previous, current and next page links.
     * @param resourceService
     * @param page
     * @param context
     * @return {@link Link}s
     */
    public static List<Link> buildPaginationLinks(IResourceService resourceService, PageImpl<?> page,
            SearchContext context) {
        List<Link> links = new ArrayList<>();
        // Build previous link
        if (page.hasPrevious()) {
            int pageNumber = context.getPageable().getPageNumber() - 1;
            links.add(buildPaginationLink(resourceService, context, page.getSize(), pageNumber, Link.REL_PREVIOUS));
        }
        // Build current page link
        links.add(buildPaginationLink(resourceService, context, page.getSize(), page.getNumber(), Link.REL_SELF));
        // Build next link
        if (page.hasNext()) {
            int pageNumber = context.getPageable().getPageNumber() - 1;
            links.add(buildPaginationLink(resourceService, context, page.getSize(), pageNumber, Link.REL_NEXT));
        }
        return links;
    }

    /**
     *
     * Return a contextual link
     * @return {@link Link}, may be null.
     */
    public static Link buildPaginationLink(IResourceService resourceService, SearchContext context, int pageSize,
            int pageNumber, String rel) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.putAll(context.getQueryParams());
        SearchContext newContext = SearchContext.build(context.getSearchType(), context.getEngineType(),
                                                       context.getHeaders(), params, context.getPageable());
        newContext.getQueryParams().put(PAGE_NUMBER, Arrays.asList(String.valueOf(pageNumber)));
        newContext.getQueryParams().put(PAGE_SIZE, Arrays.asList(String.valueOf(pageSize)));
        return buildPaginationLink(resourceService, newContext, rel);
    }

    /**
     * Return a contextual link
     * @return {@link Link}, may be null.
     */
    public static Link buildPaginationLink(IResourceService resourceService, SearchContext context, String rel) {
        if (context.getDatasetUrn().isPresent()) {
            return resourceService
                    .buildLinkWithParams(SearchEngineController.class, getMethod(context), rel,
                                         MethodParamFactory.build(String.class, context.getEngineType()),
                                         MethodParamFactory.build(String.class,
                                                                  context.getDatasetUrn().get().toString()),
                                         MethodParamFactory.build(HttpHeaders.class),
                                         MethodParamFactory.build(MultiValueMap.class, context.getQueryParams()),
                                         MethodParamFactory.build(Pageable.class));
        } else {
            return resourceService.buildLinkWithParams(SearchEngineController.class, getMethod(context), rel,
                                                       MethodParamFactory.build(String.class, context.getEngineType()),
                                                       MethodParamFactory.build(HttpHeaders.class),
                                                       MethodParamFactory.build(MultiValueMap.class,
                                                                                context.getQueryParams()),
                                                       MethodParamFactory.build(Pageable.class));
        }
    }

    /**
     * Return a contextual link
     * @return {@link Link}, may be null.
     */
    public static Link buildExtraLink(IResourceService resourceService, SearchContext context, String rel,
            String extra) {
        if (context.getDatasetUrn().isPresent()) {
            return resourceService
                    .buildLinkWithParams(SearchEngineController.class, getMethod(context) + EXTRA_METHOD_SUFFIX, rel,
                                         MethodParamFactory.build(String.class, context.getEngineType()),
                                         MethodParamFactory.build(String.class,
                                                                  context.getDatasetUrn().get().toString()),
                                         MethodParamFactory.build(String.class, extra),
                                         MethodParamFactory.build(HttpHeaders.class),
                                         MethodParamFactory.build(MultiValueMap.class, context.getQueryParams()),
                                         MethodParamFactory.build(Pageable.class));
        } else {
            return resourceService
                    .buildLinkWithParams(SearchEngineController.class, getMethod(context) + EXTRA_METHOD_SUFFIX, rel,
                                         MethodParamFactory.build(String.class, context.getEngineType()),
                                         MethodParamFactory.build(String.class, extra),
                                         MethodParamFactory.build(HttpHeaders.class),
                                         MethodParamFactory.build(MultiValueMap.class, context.getQueryParams()),
                                         MethodParamFactory.build(Pageable.class));
        }
    }

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
            case DOCUMENTS:
                return SearchEngineController.SEARCH_ALL_DOCUMENTS_METHOD;
            case DATAOBJECTS_RETURN_DATASETS:
                return SearchEngineController.SEARCH_DATAOBJECTS_DATASETS;
            default:
                throw new UnsupportedOperationException("Unsupported search type : " + context.getSearchType());
        }
    }

    /**
     * Build contextual entity links according to search context and entity type
     */
    public static List<Link> buildEntityLinks(IResourceService resourceService, SearchContext context,
            EntityFeature entity) {
        return buildEntityLinks(resourceService, context, entity.getEntityType(), entity.getId());
    }

    /**
     * Build contextual entity links according to search context and entity type
     */
    public static List<Link> buildEntityLinks(IResourceService resourceService, SearchContext context,
            EntityType entityType, UniformResourceName id) {
        List<Link> links = new ArrayList<>();

        switch (entityType) {
            case COLLECTION:
                addLink(links,
                        resourceService.buildLink(SearchEngineController.class,
                                                  SearchEngineController.GET_COLLECTION_METHOD, LinkRels.SELF,
                                                  MethodParamFactory.build(String.class, context.getEngineType()),
                                                  MethodParamFactory.build(UniformResourceName.class, id),
                                                  MethodParamFactory.build(HttpHeaders.class)));
                break;
            case DATA:
                addLink(links,
                        resourceService.buildLink(SearchEngineController.class,
                                                  SearchEngineController.GET_DATAOBJECT_METHOD, LinkRels.SELF,
                                                  MethodParamFactory.build(String.class, context.getEngineType()),
                                                  MethodParamFactory.build(UniformResourceName.class, id),
                                                  MethodParamFactory.build(HttpHeaders.class)));
                break;
            case DATASET:
                addLink(links,
                        resourceService.buildLink(SearchEngineController.class,
                                                  SearchEngineController.GET_DATASET_METHOD, LinkRels.SELF,
                                                  MethodParamFactory.build(String.class, context.getEngineType()),
                                                  MethodParamFactory.build(UniformResourceName.class, id),
                                                  MethodParamFactory.build(HttpHeaders.class)));
                // Add link to DATA OBJECTS
                addLink(links,
                        resourceService.buildLink(SearchEngineController.class,
                                                  SearchEngineController.SEARCH_ALL_DATAOBJECTS_BY_DATASET,
                                                  LINK_TO_DATAOBJECTS,
                                                  MethodParamFactory.build(String.class, context.getEngineType()),
                                                  MethodParamFactory.build(String.class, id.toString()),
                                                  MethodParamFactory.build(HttpHeaders.class),
                                                  MethodParamFactory.build(MultiValueMap.class),
                                                  MethodParamFactory.build(Pageable.class)));
                // Add link to DATASET description
                addLink(links,
                        resourceService.buildLink(SearchEngineController.class,
                                                  SearchEngineController.GET_DATASET_DESCRIPTION_METHOD,
                                                  LINK_TO_DATASET_DESCRIPTION,
                                                  MethodParamFactory.build(String.class, context.getEngineType()),
                                                  MethodParamFactory.build(UniformResourceName.class, id),
                                                  MethodParamFactory.build(String.class),
                                                  MethodParamFactory.build(HttpHeaders.class)));
                break;
            case DOCUMENT:
                addLink(links,
                        resourceService.buildLink(SearchEngineController.class,
                                                  SearchEngineController.GET_DOCUMENT_METHOD, LinkRels.SELF,
                                                  MethodParamFactory.build(String.class, context.getEngineType()),
                                                  MethodParamFactory.build(UniformResourceName.class, id),
                                                  MethodParamFactory.build(HttpHeaders.class)));
                break;
            default:
                // Nothing to do
                LOGGER.warn("Unknown entity type \"{}\"", entityType);
                break;
        }
        return links;
    }

    private static void addLink(List<Link> links, Link link) {
        if (link != null) {
            links.add(link);
        }
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
}
