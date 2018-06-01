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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
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
    public static final String TYPE_MAPPING = "/search/engines/{engineType}";

    public static final String EXTRA_MAPPING = "/{extra}";

    // Search on all entities

    public static final String SEARCH_ALL_MAPPING_EXTRA = EXTRA_MAPPING;

    // Search per entity type

    public static final String SEARCH_COLLECTIONS_MAPPING = "/collections";

    public static final String SEARCH_COLLECTIONS_MAPPING_EXTRA = SEARCH_COLLECTIONS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_DOCUMENTS_MAPPING = "/documents";

    public static final String SEARCH_DOCUMENTS_MAPPING_EXTRA = SEARCH_DOCUMENTS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_DATASETS_MAPPING = "/datasets";

    public static final String SEARCH_DATASETS_MAPPING_EXTRA = SEARCH_DATASETS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_DATAOBJECTS_MAPPING = "/dataobjects";

    public static final String SEARCH_DATAOBJECTS_MAPPING_EXTRA = SEARCH_DATAOBJECTS_MAPPING + EXTRA_MAPPING;

    // Search dataobjects on a single dataset

    public static final String SEARCH_DATASET_DATAOBJECTS_MAPPING = "/datasets/{datasetId}/dataobjects";

    public static final String SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA = SEARCH_DATASET_DATAOBJECTS_MAPPING
            + EXTRA_MAPPING;

    // Search on dataobjects replying datasets

    public static final String SEARCH_DATAOBJECTS_DATASETS_MAPPING = "/dataobjects/datasets";

    @Autowired
    private ISearchEngineDispatcher searchEngineService;

    /**
     * Search on all index regardless the entity type
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Search engines dispatcher for global search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAll(@PathVariable String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all entities delegated to engine \"{}\"", engineType);
        return searchEngineService.searchAll(engineType, headers, getDecodedParams(allParams), pageable);
    }

    /**
     * Extra mapping related to search all request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_ALL_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for global search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllExtra(@PathVariable String engineType, @PathVariable String extra,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> allParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Extra mapping \"{}\" handling delegated to engine \"{}\"", extra, engineType);
        return searchEngineService.searchAllExtra(engineType, extra, headers, getDecodedParams(allParams), pageable);
    }

    /**
     * Search on all collections
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_COLLECTIONS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for collection search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllCollections(@PathVariable String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all collections delegated to engine \"{}\"", engineType);
        return searchEngineService.searchAllCollections(engineType, headers, getDecodedParams(allParams), pageable);
    }

    /**
     * Extra mapping related to search on all collections request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_COLLECTIONS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for collection search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllCollectionsExtra(@PathVariable String engineType, @PathVariable String extra,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> allParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search all collections extra mapping \"{}\" handling delegated to engine \"{}\"", extra,
                     engineType);
        return searchEngineService.searchAllCollectionsExtra(engineType, extra, headers, getDecodedParams(allParams),
                                                             pageable);
    }

    /**
     * Search on all documents
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DOCUMENTS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for document search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDocuments(@PathVariable String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all documents delegated to engine \"{}\"", engineType);
        return searchEngineService.searchAllDocuments(engineType, headers, getDecodedParams(allParams), pageable);
    }

    /**
     * Extra mapping related to search on all documents request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DOCUMENTS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for document search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDocumentsExtra(@PathVariable String engineType, @PathVariable String extra,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> allParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search all documents extra mapping \"{}\" handling delegated to engine \"{}\"", extra,
                     engineType);
        return searchEngineService.searchAllDocumentsExtra(engineType, extra, headers, getDecodedParams(allParams),
                                                           pageable);
    }

    /**
     * Search on all datasets
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATASETS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDatasets(@PathVariable String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all datasets delegated to engine \"{}\"", engineType);
        return searchEngineService.searchAllDatasets(engineType, headers, getDecodedParams(allParams), pageable);
    }

    /**
     * Extra mapping related to search on all datasets request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATASETS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDatasetsExtra(@PathVariable String engineType, @PathVariable String extra,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> allParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search all datasets extra mapping \"{}\" handling delegated to engine \"{}\"", extra, engineType);
        return searchEngineService.searchAllDatasetsExtra(engineType, extra, headers, getDecodedParams(allParams),
                                                          pageable);
    }

    /**
     * Search on all dataobjects
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATAOBJECTS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for dataobject search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDataobjects(@PathVariable String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all dataobjects delegated to engine \"{}\"", engineType);
        return searchEngineService.searchAllDataobjects(engineType, headers, getDecodedParams(allParams), pageable);
    }

    /**
     * Extra mapping related to search on all dataobjects request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATAOBJECTS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for dataobject search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllDataobjectsExtra(@PathVariable String engineType, @PathVariable String extra,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> allParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search all dataobjects extra mapping \"{}\" handling delegated to engine \"{}\"", extra,
                     engineType);
        return searchEngineService.searchAllDataobjectsExtra(engineType, extra, headers, getDecodedParams(allParams),
                                                             pageable);
    }

    /**
     * Search dataobjects on a single dataset
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATASET_DATAOBJECTS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for single dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchSingleDataset(@PathVariable String engineType, @PathVariable String datasetId,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> allParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search dataobjects on dataset \"{}\" delegated to engine \"{}\"", datasetId, engineType);
        return searchEngineService.searchSingleDataset(engineType, datasetId, headers, getDecodedParams(allParams),
                                                       pageable);
    }

    /**
     * Extra mapping related to search on a single dataset request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for single dataset search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchSingleDatasetExtra(@PathVariable String engineType, @PathVariable String datasetId,
            @PathVariable String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search dataobjects on dataset \"{}\" extra mapping \"{}\" handling delegated to engine \"{}\"",
                     datasetId, extra, engineType);
        return searchEngineService.searchSingleDatasetExtra(engineType, datasetId, extra, headers,
                                                            getDecodedParams(allParams), pageable);
    }

    /**
     * Search dataobjects return datasets
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_DATAOBJECTS_DATASETS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for dataset search with dataobject criterions",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchDataobjectsReturnDatasets(@PathVariable String engineType,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> allParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search datasets with dataobject criterions delegated to engine \"{}\"", engineType);
        return searchEngineService.searchDataobjectsReturnDatasets(engineType, headers, getDecodedParams(allParams),
                                                                   pageable);
    }

    // FIXME : fix issue in frontend to avoid double decoding
    private MultiValueMap<String, String> getDecodedParams(MultiValueMap<String, String> allParams)
            throws SearchException {

        MultiValueMap<String, String> allDecodedParams = new LinkedMultiValueMap<>();
        for (Entry<String, List<String>> kvp : allParams.entrySet()) {
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
