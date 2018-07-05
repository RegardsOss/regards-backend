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
package fr.cnes.regards.modules.search.client;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
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
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.domain.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedResources;

/**
 * Legacy search engine client (same as {@link ISearchEngineClient} with explicit return type and fixed engine mapping)
 *
 * @author Marc Sordi
 *
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = SearchEngineMappings.TYPE_MAPPING_FOR_LEGACY, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ILegacySearchEngineClient {

    // Search on all entities

    /**
     * Search on all index regardless the entity type
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_ALL_MAPPING)
    ResponseEntity<FacettedPagedResources<Resource<EntityFeature>>> searchAll(@RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam(SearchEngineMappings.PAGE) int page,
            @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Get an entity from its URN regardless its type
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_ENTITY_MAPPING)
    ResponseEntity<Resource<EntityFeature>> getEntity(
            @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn, @RequestHeader HttpHeaders headers);

    // Collection mappings

    /**
     * Search on all collections
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING)
    ResponseEntity<FacettedPagedResources<Resource<EntityFeature>>> searchAllCollections(
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.PAGE) int page, @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on all collections request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_COLLECTIONS_PROPERTY_VALUES)
    ResponseEntity<List<String>> searchCollectionPropertyValues(
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    /**
     * Get a collection from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_COLLECTION_MAPPING)
    ResponseEntity<Resource<EntityFeature>> getCollection(
            @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn, @RequestHeader HttpHeaders headers);

    // Document mappings

    /**
     * Search on all documents
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DOCUMENTS_MAPPING)
    ResponseEntity<FacettedPagedResources<Resource<EntityFeature>>> searchAllDocuments(
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.PAGE) int page, @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on all documents request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DOCUMENTS_PROPERTY_VALUES)
    ResponseEntity<List<String>> searchDocumentPropertyValues(
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    /**
     * Get a document from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_DOCUMENT_MAPPING)
    ResponseEntity<Resource<EntityFeature>> getDocument(
            @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn, @RequestHeader HttpHeaders headers);

    // Dataset mappings

    /**
     * Search on all datasets
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASETS_MAPPING)
    ResponseEntity<FacettedPagedResources<Resource<EntityFeature>>> searchAllDatasets(
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.PAGE) int page, @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on all datasets request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASETS_PROPERTY_VALUES)
    ResponseEntity<List<String>> searchDatasetPropertyValues(
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    /**
     * Get a dataset from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_DATASET_MAPPING)
    ResponseEntity<Resource<EntityFeature>> getDataset(
            @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn, @RequestHeader HttpHeaders headers);

    // Dataobject mappings

    /**
     * Search on all dataobjects
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING)
    ResponseEntity<FacettedPagedResources<Resource<EntityFeature>>> searchAllDataobjects(
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.PAGE) int page, @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on all dataobjects request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTY_VALUES)
    ResponseEntity<List<String>> searchDataobjectPropertyValues(
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    /**
     * Get a dataobject from its URN
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_DATAOBJECT_MAPPING)
    ResponseEntity<Resource<EntityFeature>> getDataobject(
            @Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn, @RequestHeader HttpHeaders headers);

    /**
     * Compute a DocFileSummary for current user, for specified request context, for asked file types (see
     * {@link DataType})
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.DATAOBJECTS_SUMMARY_MAPPING)
    ResponseEntity<DocFilesSummary> computeDatasetsSummary(@RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.FILE_TYPES) String[] fileTypes);

    // Search dataobjects on a single dataset mapping

    /**
     * Search dataobjects on a single dataset<br/>
     *
     * This method uses a {@link String} to handle dataset URN because HATEOAS link builder cannot manage complex type
     * conversion at the moment. It does not consider custom {@link Converter}.
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING)
    ResponseEntity<FacettedPagedResources<Resource<EntityFeature>>> searchSingleDataset(
            @PathVariable(SearchEngineMappings.DATASET_URN) String datasetUrn, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams, @RequestParam(SearchEngineMappings.PAGE) int page,
            @RequestParam(SearchEngineMappings.SIZE) int size);

    /**
     * Search property values on dataobjects of a single dataset request
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_PROPERTY_VALUES)
    ResponseEntity<List<String>> searchDataobjectPropertyValuesOnDataset(
            @PathVariable(SearchEngineMappings.DATASET_URN) String datasetUrn,
            @PathVariable(SearchEngineMappings.PROPERTY_NAME) String propertyName, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.MAX_COUNT) int maxCount);

    /**
     * Compute a DocFileSummary for current user, for specified request context, for asked file types (see
     * {@link DataType}) on a single dataset
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.DATASET_DATAOBJECTS_SUMMARY_MAPPING)
    ResponseEntity<DocFilesSummary> computeDatasetsSummary(
            @PathVariable(SearchEngineMappings.DATASET_URN) String datasetUrn, @RequestHeader HttpHeaders headers,
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.FILE_TYPES) String[] fileTypes);

    // Search on dataobjects returning datasets

    /**
     * Search dataobjects returning datasets
     */
    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING)
    ResponseEntity<FacettedPagedResources<Resource<EntityFeature>>> searchDataobjectsReturnDatasets(
            @RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(SearchEngineMappings.PAGE) int page, @RequestParam(SearchEngineMappings.SIZE) int size);
}
