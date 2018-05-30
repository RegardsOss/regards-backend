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
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.search.service.SearchException;
import fr.cnes.regards.modules.search.service.engine.ISearchEngineService;

/**
 * This controller manages search engines on top of system search stack
 *
 * Each endpoint handler delegates search to related search engine plugin<br/>
 *
 * FIXME : fix the javadoc
 * To retrieve plugin configuration, use both datasetId and engineType (i.e. the search engine plugin type)
 * So only one single conf is authorized by datasetId for an engineType
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
    public static final String TYPE_MAPPING = "/search";

    public static final String ENGINE_MAPPING = "/engines/{engineType}";

    public static final String EXTRA_MAPPING = "/{extra}";

    // Search endpoints

    public static final String SEARCH_ALL_MAPPING = ENGINE_MAPPING;

    public static final String SEARCH_ALL_MAPPING_EXTRA = SEARCH_ALL_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_COLLECTIONS_MAPPING = "/collections" + ENGINE_MAPPING;

    public static final String SEARCH_COLLECTIONS_MAPPING_EXTRA = SEARCH_COLLECTIONS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_DOCUMENTS_MAPPING = "/documents" + ENGINE_MAPPING;

    public static final String SEARCH_DOCUMENTS_MAPPING_EXTRA = SEARCH_DOCUMENTS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_DATAOBJECTS_MAPPING = "/dataobjects" + ENGINE_MAPPING;

    public static final String SEARCH_DATAOBJECTS_MAPPING_EXTRA = SEARCH_DATAOBJECTS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_DATAOBJECTS_DATASETS_MAPPING = "/dataobjects/datasets" + ENGINE_MAPPING;

    public static final String SEARCH_DATAOBJECTS_DATASETS_MAPPING_EXTRA = SEARCH_DATAOBJECTS_DATASETS_MAPPING
            + EXTRA_MAPPING;

    public static final String SEARCH_DATASETS_MAPPING = "/datasets" + ENGINE_MAPPING;

    public static final String SEARCH_DATASETS_MAPPING_EXTRA = SEARCH_DATASETS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_DATASET_MAPPING = "/datasets/{datasetId}" + ENGINE_MAPPING;

    public static final String SEARCH_DATASET_MAPPING_EXTRA = SEARCH_DATASET_MAPPING + EXTRA_MAPPING;

    @Autowired
    private ISearchEngineService searchEngineService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    /**
     * Search on all index regardless the entity type
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_ALL_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for global search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAll(@PathVariable String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all entities delegated to engine \"{}\"", engineType);
        return searchEngineService.handleRequest(Searches.onAllEntities(tenantResolver.getTenant()), engineType,
                                                 headers, allParams, pageable);
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
        return searchEngineService.handleRequest(Searches.onAllEntities(tenantResolver.getTenant()), engineType, extra,
                                                 headers, allParams, pageable);
    }

    /**
     * Search on all collections
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_COLLECTIONS_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher for global search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllCollections(@PathVariable String engineType, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        LOGGER.debug("Search on all collections delegated to engine \"{}\"", engineType);
        return searchEngineService
                .handleRequest(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.COLLECTION), engineType,
                               headers, getDecodedParams(allParams), pageable);
    }

    /**
     * Extra mapping related to search on all collections request
     */
    @RequestMapping(method = RequestMethod.GET, value = SEARCH_COLLECTIONS_MAPPING_EXTRA)
    @ResourceAccess(description = "Extra mapping for global search", role = DefaultRole.PUBLIC)
    public ResponseEntity<?> searchAllCollectionsExtra(@PathVariable String engineType, @PathVariable String extra,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> allParams,
            Pageable pageable) throws ModuleException {
        LOGGER.debug("Search all collections extra mapping \"{}\" handling delegated to engine \"{}\"", extra,
                     engineType);
        return searchEngineService
                .handleRequest(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.COLLECTION), engineType,
                               extra, headers, getDecodedParams(allParams), pageable);
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
