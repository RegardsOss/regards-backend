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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
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
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IPluginService pluginService;

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

    // TODO : récupérer une instance de plugin en fonction du contexte de recherche!
    private ISearchEngine getSearchEngine(String engineType, EntityType searchType, EntityType resultType,
            String datasetId) throws ModuleException {
        // TODO retrieve right search engine according to configuration
        return new OpenSearchEngine();
    }

    private ISearchEngine getSearchEngine(String engineType) throws ModuleException {
        return getSearchEngine(engineType, null, null, null);
    }

    @Override
    public ResponseEntity<?> searchAll(String engineType, HttpHeaders headers, MultiValueMap<String, String> allParams,
            Pageable pageable) throws ModuleException {
        debugRequest(engineType, null, null, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine.searchAll(Searches.onAllEntities(tenantResolver.getTenant()), headers, allParams, pageable);
    }

    @Override
    public ResponseEntity<?> searchAllExtra(String engineType, String extra, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        debugRequest(engineType, null, extra, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine.searchAllExtra(Searches.onAllEntities(tenantResolver.getTenant()), extra, headers, allParams,
                                     pageable);
    }

    @Override
    public ResponseEntity<?> searchAllCollections(String engineType, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        debugRequest(engineType, null, null, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine.searchAllCollections(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.COLLECTION),
                                           headers, allParams, pageable);
    }

    @Override
    public ResponseEntity<?> searchAllCollectionsExtra(String engineType, String extra, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        debugRequest(engineType, null, extra, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine
                .searchAllCollectionsExtra(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.COLLECTION),
                                           extra, headers, allParams, pageable);
    }

    @Override
    public ResponseEntity<?> searchAllDocuments(String engineType, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        debugRequest(engineType, null, null, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine.searchAllDocuments(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DOCUMENT),
                                         headers, allParams, pageable);
    }

    @Override
    public ResponseEntity<?> searchAllDocumentsExtra(String engineType, String extra, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        debugRequest(engineType, null, extra, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine.searchAllDocumentsExtra(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DOCUMENT),
                                              extra, headers, allParams, pageable);
    }

    @Override
    public ResponseEntity<?> searchAllDatasets(String engineType, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        debugRequest(engineType, null, null, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine.searchAllDatasets(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATASET),
                                        headers, allParams, pageable);
    }

    @Override
    public ResponseEntity<?> searchAllDatasetsExtra(String engineType, String extra, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        debugRequest(engineType, null, extra, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine.searchAllDatasetsExtra(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATASET),
                                             extra, headers, allParams, pageable);
    }

    @Override
    public ResponseEntity<?> searchAllDataobjects(String engineType, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        debugRequest(engineType, null, null, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine.searchAllDataobjects(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATA),
                                           headers, allParams, pageable);
    }

    @Override
    public ResponseEntity<?> searchAllDataobjectsExtra(String engineType, String extra, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        debugRequest(engineType, null, extra, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine.searchAllDataobjectsExtra(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATA),
                                                extra, headers, allParams, pageable);
    }

    @Override
    public ResponseEntity<?> searchSingleDataset(String engineType, String datasetId, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        debugRequest(engineType, datasetId, null, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine.searchSingleDataset(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATA),
                                          datasetId, headers, allParams, pageable);
    }

    @Override
    public ResponseEntity<?> searchSingleDatasetExtra(String engineType, String datasetId, String extra,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        debugRequest(engineType, datasetId, extra, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine.searchSingleDatasetExtra(Searches.onSingleEntity(tenantResolver.getTenant(), EntityType.DATA),
                                               datasetId, extra, headers, allParams, pageable);
    }

    @Override
    public ResponseEntity<?> searchDataobjectsReturnDatasets(String engineType, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) throws ModuleException {
        debugRequest(engineType, null, null, headers, allParams, pageable);
        ISearchEngine engine = getSearchEngine(engineType);
        return engine.searchDataobjectsReturnDatasets(
                                                      Searches.onSingleEntityReturningJoinEntity(tenantResolver
                                                              .getTenant(), EntityType.DATA, EntityType.DATASET),
                                                      headers, allParams, pageable);
    }

    /**
     * Log request properties in DEBUG level
     */
    private void debugRequest(String engineType, String datasetId, String extra, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Handling request for engine : {}", engineType);
            if (datasetId != null) {
                LOGGER.debug("Searching data objects on dataset : {}", datasetId);
            }
            if (extra != null) {
                LOGGER.debug("Handling request extra path : {}", extra);
            }
            headers.forEach((key, values) -> LOGGER.debug("Header : {} -> {}", key, values.toString()));
            allParams.forEach((key, values) -> LOGGER.debug("Query param : {} -> {}", key, values.toString()));
            LOGGER.debug(pageable.toString());
        }
    }
}
