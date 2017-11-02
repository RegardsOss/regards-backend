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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;

/**
 * This controller manages search engines on top of system search stack
 *
 * @author Marc Sordi
 *
 */
@RestController
public class SearchEngineController {

    public static final String ENGINE_MAPPING = "/engines/{engineType}";

    // TODO gérer le path parameter optionnel soit dans RequestMapping value auquel cas, il faut revoir le starter
    // sécurité pour gérer plusieurs path soit en doublant les méthodes
    public static final String EXTRA_MAPPING = "/{extra}";

    // Search endpoints

    public static final String SEARCH_ALL_MAPPING = ENGINE_MAPPING;

    public static final String SEARCH_COLLECTIONS_MAPPING = "/collections" + ENGINE_MAPPING;

    public static final String SEARCH_DOCUMENTS_MAPPING = "/documents" + ENGINE_MAPPING;

    public static final String SEARCH_DATAOBJECTS_MAPPING = "/dataobjects" + ENGINE_MAPPING;

    public static final String SEARCH_DATAOBJECTS_DATASETS_MAPPING = "/dataobjects/datasets" + ENGINE_MAPPING;

    public static final String SEARCH_DATASETS_MAPPING = "/datasets" + ENGINE_MAPPING;

    public static final String SEARCH_DATASET_MAPPING = "/datasets/{datasetId}" + ENGINE_MAPPING;

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineController.class);

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Search engines dispatcher")
    public ResponseEntity<?> search(@PathVariable final String datasetId, @PathVariable final String engineType,
            @RequestParam final Map<String, String> allParams, @RequestHeader HttpHeaders headers)
            throws ModuleException {
        // Delegate search to related search engine
        // To retrieve plugin configuration, use both datasetId and engineType (i.e. the search engine plugin type)
        // So only one single conf is authorized by datasetId for an engineType
        // TODO
        LOGGER.info("You enter search engines hell!");
        LOGGER.info("Dataset : {} / Engine type : {}", datasetId, engineType);
        allParams.forEach((k, v) -> LOGGER.info("KVP : {} = {}", k, v));
        return ResponseEntity.ok("God exists!");
    }

    @RequestMapping(method = RequestMethod.GET, path = EXTRA_MAPPING)
    @ResourceAccess(description = "Search engines dispatcher with extra path")
    public ResponseEntity<?> searchExtra(@PathVariable final String datasetId, @PathVariable final String engineType,
            @PathVariable final String extra, @RequestParam final Map<String, String> allParams)
            throws ModuleException {
        // Delegate search to related search engine
        // To retrieve plugin configuration, use both datasetId and engineType (i.e. the search engine plugin type)
        // So only one single conf is authorized by datasetId for an engineType
        // TODO
        LOGGER.info("You enter search engines hell!");
        LOGGER.info("Dataset : {} / Engine type : {} / Extra : {}", datasetId, engineType, extra);
        allParams.forEach((k, v) -> LOGGER.info("KVP : {} = {}", k, v));
        return ResponseEntity.ok("God exists!");
    }

}
