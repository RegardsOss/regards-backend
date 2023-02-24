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

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;

/**
 * Generic search engine client
 *
 * @author Marc Sordi
 */
@RestClient(name = "rs-catalog", contextId = "rs-catalog.search-engine.client")
public interface ISearchEngineClient {

    // Search on all entities

    /**
     * Search on all index regardless the entity type
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_ALL_MAPPING,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchAll(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                @RequestHeader HttpHeaders headers,
                                @RequestParam MultiValueMap<String, String> queryParams,
                                @RequestParam(SearchEngineMappings.PAGE) int page,
                                @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Extra mapping related to search all request
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_ALL_MAPPING_EXTRA,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchAllExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                     @PathVariable(SearchEngineMappings.EXTRA) String extra,
                                     @RequestHeader HttpHeaders headers,
                                     @RequestParam MultiValueMap<String, String> queryParams,
                                     @RequestParam(SearchEngineMappings.PAGE) int page,
                                     @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Get an entity from its URN regardless its type
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.GET_ENTITY_MAPPING,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> getEntity(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn,
                                @RequestHeader HttpHeaders headers);

    // Collection mappings

    /**
     * Search on all collections
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchAllCollections(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                           @RequestHeader HttpHeaders headers,
                                           @RequestParam MultiValueMap<String, String> queryParams,
                                           @RequestParam(SearchEngineMappings.PAGE) int page,
                                           @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Extra mapping related to search on all collections request
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING_EXTRA,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchAllCollectionsExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                                @PathVariable(SearchEngineMappings.EXTRA) String extra,
                                                @RequestHeader HttpHeaders headers,
                                                @RequestParam MultiValueMap<String, String> queryParams,
                                                @RequestParam(SearchEngineMappings.PAGE) int page,
                                                @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on all collections request
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_PROPERTY_VALUES,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchCollectionPropertyValues(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                                     @PathVariable(SearchEngineMappings.PROPERTY_NAME)
                                                     String propertyName,
                                                     @RequestHeader HttpHeaders headers,
                                                     @RequestParam MultiValueMap<String, String> queryParams,
                                                     @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    /**
     * Get a collection from its URN
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.GET_COLLECTION_MAPPING,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> getCollection(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                    @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn,
                                    @RequestHeader HttpHeaders headers);

    // Dataset mappings

    /**
     * Search on all datasets
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASETS_MAPPING,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchAllDatasets(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                        @RequestHeader HttpHeaders headers,
                                        @RequestParam MultiValueMap<String, String> queryParams,
                                        @RequestParam(SearchEngineMappings.PAGE) int page,
                                        @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Extra mapping related to search on all datasets request
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASETS_MAPPING_EXTRA,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchAllDatasetsExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                             @PathVariable(SearchEngineMappings.EXTRA) String extra,
                                             @RequestHeader HttpHeaders headers,
                                             @RequestParam MultiValueMap<String, String> queryParams,
                                             @RequestParam(SearchEngineMappings.PAGE) int page,
                                             @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on all datasets request
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASETS_PROPERTY_VALUES,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchDatasetPropertyValues(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                                  @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName,
                                                  @RequestHeader HttpHeaders headers,
                                                  @RequestParam MultiValueMap<String, String> queryParams,
                                                  @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    /**
     * Get a dataset from its URN
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.GET_DATASET_MAPPING,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> getDataset(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                 @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn,
                                 @RequestHeader HttpHeaders headers);

    // Dataobject mappings

    /**
     * Search on all dataobjects
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchAllDataobjects(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                           @RequestHeader HttpHeaders headers,
                                           @RequestParam MultiValueMap<String, String> queryParams,
                                           @RequestParam(SearchEngineMappings.PAGE) int page,
                                           @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Extra mapping related to search on all dataobjects request
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING_EXTRA,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchAllDataobjectsExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                                @PathVariable(SearchEngineMappings.EXTRA) String extra,
                                                @RequestHeader HttpHeaders headers,
                                                @RequestParam MultiValueMap<String, String> queryParams,
                                                @RequestParam(SearchEngineMappings.PAGE) int page,
                                                @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on all dataobjects request
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTY_VALUES,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchDataobjectPropertyValues(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                                     @PathVariable(SearchEngineMappings.PROPERTY_NAME)
                                                     String propertyName,
                                                     @RequestHeader HttpHeaders headers,
                                                     @RequestParam MultiValueMap<String, String> queryParams,
                                                     @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    /**
     * Get a dataobject from its URN
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.GET_DATAOBJECT_MAPPING,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> getDataobject(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                    @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn,
                                    @RequestHeader HttpHeaders headers);

    // Search dataobjects on a single dataset mapping

    /**
     * Search dataobjects on a single dataset<br/>
     * <p>
     * This method uses a {@link String} to handle dataset URN because HATEOAS link builder cannot manage complex type
     * conversion at the moment. It does not consider custom converter.
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchSingleDataset(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                          @PathVariable(SearchEngineMappings.DATASET_URN) String datasetUrn,
                                          @RequestHeader HttpHeaders headers,
                                          @RequestParam MultiValueMap<String, String> queryParams,
                                          @RequestParam(SearchEngineMappings.PAGE) int page,
                                          @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Extra mapping related to search on a single dataset request<br/>
     * <p>
     * This method uses a {@link String} to handle dataset URN because HATEOAS link builder cannot manage complex type
     * conversion at the moment. It does not consider custom converter.
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING
                       + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchSingleDatasetExtra(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                               @PathVariable(SearchEngineMappings.DATASET_URN) String datasetUrn,
                                               @PathVariable(SearchEngineMappings.EXTRA) String extra,
                                               @RequestHeader HttpHeaders headers,
                                               @RequestParam MultiValueMap<String, String> queryParams,
                                               @RequestParam(SearchEngineMappings.PAGE) int page,
                                               @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on dataobjects of a single dataset request
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING
                       + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_PROPERTY_VALUES,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchDataobjectPropertyValuesOnDataset(
        @PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
        @PathVariable(SearchEngineMappings.DATASET_URN) String datasetUrn,
        @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName,
        @RequestHeader HttpHeaders headers,
        @RequestParam MultiValueMap<String, String> queryParams,
        @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    // Search on dataobjects returning datasets

    /**
     * Search dataobjects returning datasets
     */
    @GetMapping(path = SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> searchDataobjectsReturnDatasets(@PathVariable(SearchEngineMappings.ENGINE_TYPE) String engineType,
                                                      @RequestHeader HttpHeaders headers,
                                                      @RequestParam MultiValueMap<String, String> queryParams,
                                                      @RequestParam(SearchEngineMappings.PAGE) int page,
                                                      @RequestParam(SearchEngineMappings.SIZE) int size);
}
