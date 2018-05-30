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
package fr.cnes.regards.modules.search.service.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import fr.cnes.regards.modules.indexer.domain.IIndexable;
import fr.cnes.regards.modules.indexer.domain.SearchKey;
import fr.cnes.regards.modules.search.domain.plugin.ISearchEngine;
import fr.cnes.regards.modules.search.service.engine.plugin.OpenSearchEngine;

/**
 * Search engine service
 *
 * @author Marc Sordi
 *
 */
@Service
public class SearchEngineService implements ISearchEngineService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineService.class);

    @Override
    public <S, R extends IIndexable> ResponseEntity<?> handleRequest(SearchKey<S, R> searchKey, String engineType,
            HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) {
        debugRequest(engineType, null, headers, allParams, pageable);

        // Delegate request handling to search engine plugin
        // TODO
        // - récupérer la conf de plugin correspondant au endpoint courant et au type d'engine
        // - gérer erreur si engine non reconnu
        ISearchEngine engine = new OpenSearchEngine();
        return engine.handleRequest(engineType, null, headers, allParams, pageable);
    }

    @Override
    public <S, R extends IIndexable> ResponseEntity<?> handleRequest(SearchKey<S, R> searchKey, String engineType,
            String extra, HttpHeaders headers, MultiValueMap<String, String> allParams, Pageable pageable) {
        debugRequest(engineType, extra, headers, allParams, pageable);
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Log request properties in DEBUG level
     */
    private void debugRequest(String engineType, String extra, HttpHeaders headers,
            MultiValueMap<String, String> allParams, Pageable pageable) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Handling request for engine : {}", engineType);
            if (extra != null) {
                LOGGER.debug("Handling request extra path : {}", extra);
            }
            headers.forEach((key, values) -> LOGGER.debug("Header : {} -> {}", key, values.toString()));
            allParams.forEach((key, values) -> LOGGER.debug("Query param : {} -> {}", key, values.toString()));
            LOGGER.debug(pageable.toString());
        }
    }
}
