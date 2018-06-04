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
package fr.cnes.regards.modules.search.rest.engine;

import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;

/**
 * Search engine service contract<br/>
 * The service dispatches request to right search engine.
 *
 * @author Marc Sordi
 *
 */
public interface ISearchEngineDispatcher {

    /**
     * Dispatch request to the right search engine according to specified search context
     */
    <T> ResponseEntity<T> dispatchRequest(SearchContext context) throws ModuleException;

    // /**
    // * Dispatch request to right search engine
    // * @param searchType entity type to search (may be null to search ALL entities)
    // * @param returnType entity type to return (only available for dataobject search returning datasets)
    // * @param engineType search engine type identifier
    // * @param datasetId dataset identifier (may be null, only available to search dataobjects on this
    // * specified dataset)
    // * @param extra extra path (may be null)
    // * @param headers request headers
    // * @param allParams request parameters
    // * @param pageable pagination properties
    // * @return a response object given by search engine plugin
    // */
    // <S, R extends IIndexable> ResponseEntity<?> dispatchRequest(EntityType searchType, EntityType returnType,
    // String engineType, String datasetId, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable);

    // /**
    // * Dispatch request where search type is return type
    // */
    // default <S, R extends IIndexable> ResponseEntity<?> dispatchRequest(EntityType searchType, String engineType,
    // String extra, HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) {
    // return dispatchRequest(searchType, searchType, engineType, null, extra, headers, allParams, pageable);
    // }
    //
    // /**
    // * Dispatch request where search type is not return type
    // */
    // default <S, R extends IIndexable> ResponseEntity<?> dispatchRequest(EntityType searchType, EntityType returnType,
    // String engineType, String extra, HttpHeaders headers, MultiValueMap<String, String> allParams,
    // Pageable pageable) {
    // return dispatchRequest(searchType, returnType, engineType, null, extra, headers, allParams, pageable);
    // }

    // ResponseEntity<?> searchAll(String engineType, HttpHeaders headers, MultiValueMap<String, String> allParams,
    // Pageable pageable) throws ModuleException;
    //
    // ResponseEntity<?> searchAllExtra(String engineType, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;
    //
    // ResponseEntity<?> searchAllCollections(String engineType, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;
    //
    // ResponseEntity<?> searchAllCollectionsExtra(String engineType, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;
    //
    // ResponseEntity<?> searchAllDocuments(String engineType, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;
    //
    // ResponseEntity<?> searchAllDocumentsExtra(String engineType, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;
    //
    // ResponseEntity<?> searchAllDatasets(String engineType, HttpHeaders headers, MultiValueMap<String, String>
    // allParams,
    // Pageable pageable) throws ModuleException;
    //
    // ResponseEntity<?> searchAllDatasetsExtra(String engineType, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;
    //
    // ResponseEntity<?> searchAllDataobjects(String engineType, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;
    //
    // ResponseEntity<?> searchAllDataobjectsExtra(String engineType, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;
    //
    // ResponseEntity<?> searchSingleDataset(String engineType, String datasetId, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;
    //
    // ResponseEntity<?> searchSingleDatasetExtra(String engineType, String datasetId, String extra, HttpHeaders
    // headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;
    //
    // ResponseEntity<?> searchDataobjectsReturnDatasets(String engineType, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException;
}
