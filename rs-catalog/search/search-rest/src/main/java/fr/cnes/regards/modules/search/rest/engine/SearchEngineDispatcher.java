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

import java.util.HashMap;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchEngine;

/**
 * Search engine service dispatcher.<br/>
 *
 * Each methods acts as a proxy for search engine.<br/>
 * <ul>
 * <li>First, context debugging may be display</li>
 * <li>The system look for a plugin instance</li>
 * <li>The system dispatches search to plugin instance</li>
 * </ul>
 *
 * @author Marc Sordi
 *
 */
@Service
public class SearchEngineDispatcher implements ISearchEngineDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineDispatcher.class);

    @Autowired
    private Validator validator;

    @Autowired
    private IPluginService pluginService;

    @SuppressWarnings("unchecked")
    @Override
    public <T> ResponseEntity<T> dispatchRequest(SearchContext context) throws ModuleException {

        // Validate search context
        Errors errors = new MapBindingResult(new HashMap<>(), "searchContext");
        validator.validate(context, errors);
        if (errors.hasErrors()) {
            StringJoiner joiner = new StringJoiner(". ");
            errors.getAllErrors().forEach(error -> {
                joiner.add(error.toString());
            });
            throw new EntityInvalidException(joiner.toString());
        }

        // Debugging
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Handling request for engine : {}", context.getEngineType());
            if (context.getDatasetId().isPresent()) {
                LOGGER.debug("Searching data objects on dataset : {}", context.getDatasetId().get());
            }
            if (context.getExtra().isPresent()) {
                LOGGER.debug("Handling request extra path : {}", context.getExtra().get());
            }
            context.getHeaders().forEach((key, values) -> LOGGER.debug("Header : {} -> {}", key, values.toString()));
            context.getQueryParams()
                    .forEach((key, values) -> LOGGER.debug("Query param : {} -> {}", key, values.toString()));
            LOGGER.debug(context.getPageable().toString());
        }

        // Retrieve search engine plugin from search context
        ISearchEngine<?, ?> engine = getSearchEngine(context);
        if (context.getExtra().isPresent()) {
            return (ResponseEntity<T>) engine.extra(context);
        } else {
            return (ResponseEntity<T>) engine.search(context);
        }
    }

    private ISearchEngine<?, ?> getSearchEngine(SearchContext context) throws ModuleException {
        // TODO Retrieve search engine plugin from search context
        return new OpenSearchEngine();
    }

    // @Override
    // public <S, R extends IIndexable> ResponseEntity<?> dispatchRequest(EntityType searchType, EntityType resultType,
    // String engineType, String datasetId, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) {
    // debugRequest(engineType, datasetId, extra, headers, allParams, pageable);
    //
    // // Check parameters consistency
    // // ?
    //
    // // Delegate request handling to search engine plugin
    // // TODO
    // // - récupérer la conf de plugin correspondant au endpoint courant et au type d'engine
    // // - gérer erreur si engine non reconnu
    // ISearchEngine engine = new OpenSearchEngine();
    // return engine.handleRequest(engineType, extra, headers, allParams, pageable);
    // }

    // // TODO : récupérer une instance de plugin en fonction du contexte de recherche!
    // private ISearchEngineOld getSearchEngine(String engineType, EntityType searchType, EntityType resultType,
    // String datasetId) throws ModuleException {
    // // TODO retrieve right search engine according to configuration
    // return new OpenSearchEngine();
    // }
    //
    // private ISearchEngineOld getSearchEngine(String engineType) throws ModuleException {
    // return getSearchEngine(engineType, null, null, null);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchAll(String engineType, HttpHeaders headers, MultiValueMap<String, String>
    // allParams,
    // Pageable pageable) throws ModuleException {
    // debugRequest(engineType, null, null, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine.searchAll(Searches.onAllEntities(tenantResolver.getTenant()), headers, allParams, pageable);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchAllExtra(String engineType, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
    // debugRequest(engineType, null, extra, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine.searchAllExtra(Searches.onAllEntities(tenantResolver.getTenant()), extra, headers, allParams,
    // pageable);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchAllCollections(String engineType, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
    // debugRequest(engineType, null, null, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine.searchAllCollections(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.COLLECTION),
    // headers, allParams, pageable);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchAllCollectionsExtra(String engineType, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
    // debugRequest(engineType, null, extra, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine
    // .searchAllCollectionsExtra(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.COLLECTION),
    // extra, headers, allParams, pageable);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchAllDocuments(String engineType, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
    // debugRequest(engineType, null, null, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine.searchAllDocuments(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DOCUMENT),
    // headers, allParams, pageable);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchAllDocumentsExtra(String engineType, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
    // debugRequest(engineType, null, extra, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine.searchAllDocumentsExtra(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DOCUMENT),
    // extra, headers, allParams, pageable);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchAllDatasets(String engineType, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
    // debugRequest(engineType, null, null, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine.searchAllDatasets(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATASET),
    // headers, allParams, pageable);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchAllDatasetsExtra(String engineType, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
    // debugRequest(engineType, null, extra, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine.searchAllDatasetsExtra(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATASET),
    // extra, headers, allParams, pageable);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchAllDataobjects(String engineType, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
    // debugRequest(engineType, null, null, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine.searchAllDataobjects(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATA),
    // headers, allParams, pageable);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchAllDataobjectsExtra(String engineType, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
    // debugRequest(engineType, null, extra, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine.searchAllDataobjectsExtra(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATA),
    // extra, headers, allParams, pageable);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchSingleDataset(String engineType, String datasetId, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
    // debugRequest(engineType, datasetId, null, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine.searchSingleDataset(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATA),
    // datasetId, headers, allParams, pageable);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchSingleDatasetExtra(String engineType, String datasetId, String extra,
    // HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
    // debugRequest(engineType, datasetId, extra, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine.searchSingleDatasetExtra(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATA),
    // datasetId, extra, headers, allParams, pageable);
    // }
    //
    // @Override
    // public ResponseEntity<?> searchDataobjectsReturnDatasets(String engineType, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
    // debugRequest(engineType, null, null, headers, allParams, pageable);
    // ISearchEngineOld engine = getSearchEngine(engineType);
    // return engine.searchDataobjectsReturnDatasets(
    // Searches.onSingleEntityReturningJoinEntity(tenantResolver
    // .getTenant(), EntityType.DATA, EntityType.DATASET),
    // headers, allParams, pageable);
    // }
    //
    // /**
    // * Log request properties in DEBUG level
    // */
    // private void debugRequest(String engineType, String datasetId, String extra, HttpHeaders headers,
    // MultiValueMap<String, String> allParams, Pageable pageable) {
    // if (LOGGER.isDebugEnabled()) {
    // LOGGER.debug("Handling request for engine : {}", engineType);
    // if (datasetId != null) {
    // LOGGER.debug("Searching data objects on dataset : {}", datasetId);
    // }
    // if (extra != null) {
    // LOGGER.debug("Handling request extra path : {}", extra);
    // }
    // headers.forEach((key, values) -> LOGGER.debug("Header : {} -> {}", key, values.toString()));
    // allParams.forEach((key, values) -> LOGGER.debug("Query param : {} -> {}", key, values.toString()));
    // LOGGER.debug(pageable.toString());
    // }
    // }
}
