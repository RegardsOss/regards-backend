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
package fr.cnes.regards.modules.search.client;

import javax.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;

/**
 * Generic search engine client
 *
 * @author Marc Sordi
 *
 */
@RestClient(name = "rs-catalog", contextId = "rs-catalog.search-engine.client")
@RequestMapping(value = SearchEngineMappings.TYPE_MAPPING, produces = MediaType.APPLICATION_JSON_VALUE)
public interface ISearchEngineClient {

    // Search on all entities

    /**
     * Search on all index regardless the entity type
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_ALL_MAPPING)
    ResponseEntity<?> searchAll(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.PAGE) int page, @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Extra mapping related to search all request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_ALL_MAPPING_EXTRA)
    ResponseEntity<?> searchAllExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.EXTRA) String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam(SearchEngineMappings.PAGE) int page,
            @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Get an entity from its URN regardless its type
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_ENTITY_MAPPING)
    ResponseEntity<?> getEntity(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn, @RequestHeader HttpHeaders headers);

    // Collection mappings

    /**
     * Search on all collections
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING)
    ResponseEntity<?> searchAllCollections(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.PAGE) int page, @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Extra mapping related to search on all collections request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING_EXTRA)
    ResponseEntity<?> searchAllCollectionsExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.EXTRA) String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam(SearchEngineMappings.PAGE) int page,
            @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on all collections request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_COLLECTIONS_PROPERTY_VALUES)
    ResponseEntity<?> searchCollectionPropertyValues(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    /**
     * Get a collection from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_COLLECTION_MAPPING)
    ResponseEntity<?> getCollection(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn, @RequestHeader HttpHeaders headers);

    // Dataset mappings

    /**
     * Search on all datasets
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASETS_MAPPING)
    ResponseEntity<?> searchAllDatasets(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.PAGE) int page, @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Extra mapping related to search on all datasets request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASETS_MAPPING_EXTRA)
    ResponseEntity<?> searchAllDatasetsExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.EXTRA) String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam(SearchEngineMappings.PAGE) int page,
            @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on all datasets request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASETS_PROPERTY_VALUES)
    ResponseEntity<?> searchDatasetPropertyValues(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    /**
     * Get a dataset from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_DATASET_MAPPING)
    ResponseEntity<?> getDataset(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn, @RequestHeader HttpHeaders headers);

    // Dataobject mappings

    /**
     * Search on all dataobjects
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING)
    ResponseEntity<?> searchAllDataobjects(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.PAGE) int page, @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Extra mapping related to search on all dataobjects request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING_EXTRA)
    ResponseEntity<?> searchAllDataobjectsExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.EXTRA) String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam(SearchEngineMappings.PAGE) int page,
            @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on all dataobjects request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTY_VALUES)
    ResponseEntity<?> searchDataobjectPropertyValues(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    /**
     * Get a dataobject from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_DATAOBJECT_MAPPING)
    ResponseEntity<?> getDataobject(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn, @RequestHeader HttpHeaders headers);

    // Search dataobjects on a single dataset mapping

    /**
     * Search dataobjects on a single dataset<br/>
     *
     * This method uses a {@link String} to handle dataset URN because HATEOAS link builder cannot manage complex type
     * conversion at the moment. It does not consider custom converter.
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING)
    ResponseEntity<?> searchSingleDataset(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.DATASET_URN) String datasetUrn, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam(SearchEngineMappings.PAGE) int page,
            @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Extra mapping related to search on a single dataset request<br/>
     *
     * This method uses a {@link String} to handle dataset URN because HATEOAS link builder cannot manage complex type
     * conversion at the moment. It does not consider custom converter.
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA)
    ResponseEntity<?> searchSingleDatasetExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.DATASET_URN) String datasetUrn,
            @PathVariable(SearchEngineMappings.EXTRA) String extra, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam(SearchEngineMappings.PAGE) int page,
            @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on dataobjects of a single dataset request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_PROPERTY_VALUES)
    ResponseEntity<?> searchDataobjectPropertyValuesOnDataset(
            @PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @PathVariable(SearchEngineMappings.DATASET_URN) String datasetUrn,
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    // Search on dataobjects returning datasets

    /**
     * Search dataobjects returning datasets
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING)
    ResponseEntity<?> searchDataobjectsReturnDatasets(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.PAGE) int page, @RequestParam(SearchEngineMappings.SIZE) int size);
}
