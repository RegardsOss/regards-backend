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
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedModel;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Legacy search engine client (same as {@link ISearchEngineClient} with explicit return type and fixed engine mapping)
 *
 * @author Marc Sordi
 */
@RestClient(name = "rs-catalog", contextId = "rs-catalog.legacy-search-engine.client")
public interface ILegacySearchEngineClient {

    String ROOT_PATH = TYPE_MAPPING_FOR_LEGACY;

    // Search on all entities

    /**
     * Search on all index regardless the entity type
     */
    @GetMapping(path = ROOT_PATH + SEARCH_ALL_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchAll(@RequestHeader HttpHeaders headers,
                                                                             @SpringQueryMap
                                                                             MultiValueMap<String, String> queryParams,
                                                                             @RequestParam(name = SEARCH_REQUEST_PARSER,
                                                                                           required = false)
                                                                             String engineParserType,
                                                                             @RequestParam(PAGE) int page,
                                                                             @RequestParam(SIZE) int size);

    /**
     * Get an entity from its URN regardless its type
     */
    @GetMapping(path = ROOT_PATH + GET_ENTITY_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<EntityFeature>> getEntity(
        @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn, @RequestHeader HttpHeaders headers);

    // Collection mappings

    /**
     * Search on all collections
     */
    @GetMapping(path = ROOT_PATH + SEARCH_COLLECTIONS_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchAllCollections(
        @RequestHeader HttpHeaders headers,
        @SpringQueryMap MultiValueMap<String, String> queryParams,
        @RequestParam(name = SEARCH_REQUEST_PARSER, required = false) String engineParserType,
        @RequestParam(PAGE) int page,
        @RequestParam(SIZE) int size);

    /**
     * Search property values on all collections request
     */
    @GetMapping(path = ROOT_PATH + SEARCH_COLLECTIONS_PROPERTY_VALUES, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<List<String>> searchCollectionPropertyValues(@PathVariable(PROPERTY_NAME) String propertyName,
                                                                @RequestHeader HttpHeaders headers,
                                                                @SpringQueryMap
                                                                MultiValueMap<String, String> queryParams,
                                                                @RequestParam(MAX_COUNT) int maxCount);

    /**
     * Get a collection from its URN
     */
    @GetMapping(path = ROOT_PATH + GET_COLLECTION_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<EntityFeature>> getCollection(@Valid @PathVariable(URN) UniformResourceName urn,
                                                             @RequestHeader HttpHeaders headers);

    // Dataset mappings

    /**
     * Search on all datasets
     */
    @GetMapping(path = ROOT_PATH + SEARCH_DATASETS_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchAllDatasets(@RequestHeader HttpHeaders headers,
                                                                                     @SpringQueryMap
                                                                                     MultiValueMap<String, String> queryParams,
                                                                                     @RequestParam(name = SEARCH_REQUEST_PARSER,
                                                                                                   required = false)
                                                                                     String engineParserType,
                                                                                     @RequestParam(PAGE) int page,
                                                                                     @RequestParam(SIZE) int size);

    /**
     * Search property values on all datasets request
     */
    @GetMapping(path = ROOT_PATH + SEARCH_DATASETS_PROPERTY_VALUES, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<List<String>> searchDatasetPropertyValues(@PathVariable(PROPERTY_NAME) String propertyName,
                                                             @RequestHeader HttpHeaders headers,
                                                             @SpringQueryMap MultiValueMap<String, String> queryParams,
                                                             @RequestParam(MAX_COUNT) int maxCount);

    /**
     * Get a dataset from its URN
     */
    @GetMapping(value = ROOT_PATH + GET_DATASET_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<EntityFeature>> getDataset(@Valid @PathVariable(URN) UniformResourceName urn,
                                                          @RequestHeader HttpHeaders headers);

    // Dataobject mappings

    /**
     * Search on all dataobjects
     */
    @GetMapping(path = ROOT_PATH + SEARCH_DATAOBJECTS_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchAllDataobjects(
        @RequestHeader HttpHeaders headers,
        @SpringQueryMap MultiValueMap<String, String> queryParams,
        @RequestParam(name = SEARCH_REQUEST_PARSER, required = false) String engineParserType,
        @RequestParam(PAGE) int page,
        @RequestParam(SIZE) int size);

    /**
     * Search property values on all dataobjects request
     */
    @GetMapping(path = ROOT_PATH + SEARCH_DATAOBJECTS_PROPERTY_VALUES, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<List<String>> searchDataobjectPropertyValues(@PathVariable(PROPERTY_NAME) String propertyName,
                                                                @RequestHeader HttpHeaders headers,
                                                                @SpringQueryMap
                                                                MultiValueMap<String, String> queryParams,
                                                                @RequestParam(MAX_COUNT) int maxCount);

    /**
     * Get a dataobject from its URN
     */
    @GetMapping(path = ROOT_PATH + GET_DATAOBJECT_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<EntityFeature>> getDataobject(@Valid @PathVariable(URN) UniformResourceName urn,
                                                             @RequestHeader HttpHeaders headers);

    // Search dataobjects on a single dataset mapping

    /**
     * Search dataobjects on a single dataset<br/>
     * <p>
     * This method uses a {@link String} to handle dataset URN because HATEOAS link builder cannot manage complex type
     * conversion at the moment. It does not consider custom converter.
     */
    @GetMapping(path = ROOT_PATH + SEARCH_DATASET_DATAOBJECTS_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchSingleDataset(
        @PathVariable(DATASET_URN) String datasetUrn,
        @RequestHeader HttpHeaders headers,
        @SpringQueryMap MultiValueMap<String, String> queryParams,
        @RequestParam(name = SEARCH_REQUEST_PARSER, required = false) String engineParserType,
        @RequestParam(PAGE) int page,
        @RequestParam(SIZE) int size);

    /**
     * Search property values on dataobjects of a single dataset request
     */
    @GetMapping(path = ROOT_PATH + SEARCH_DATASET_DATAOBJECTS_PROPERTY_VALUES, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<List<String>> searchDataobjectPropertyValuesOnDataset(@PathVariable(DATASET_URN) String datasetUrn,
                                                                         @PathVariable(PROPERTY_NAME)
                                                                         String propertyName,
                                                                         @RequestHeader HttpHeaders headers,
                                                                         @SpringQueryMap
                                                                         MultiValueMap<String, String> queryParams,
                                                                         @RequestParam(MAX_COUNT) int maxCount);

    // Search on dataobjects returning datasets

    /**
     * Search dataobjects returning datasets
     */
    @GetMapping(path = ROOT_PATH + SEARCH_DATAOBJECTS_DATASETS_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<FacettedPagedModel<EntityModel<EntityFeature>>> searchDataobjectsReturnDatasets(
        @RequestHeader HttpHeaders headers,
        @SpringQueryMap MultiValueMap<String, String> queryParams,
        @RequestParam(name = SEARCH_REQUEST_PARSER, required = false) String engineParserType,
        @RequestParam(PAGE) int page,
        @RequestParam(SIZE) int size);
}
