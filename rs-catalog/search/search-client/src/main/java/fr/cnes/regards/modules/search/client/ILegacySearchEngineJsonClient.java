/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.gson.JsonObject;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;

/**
 * Feign client to call SearchController methods but with JsonObject as result types.
 * This client is mostly called by rs-access AccessSearchController (to avoid calling directly SearchController from
 * the front)
 * @author oroussel
 * @author Sébastien Binda
 */
@RestClient(name = "rs-catalog", contextId = "rs-catalog.legacy-search-engind-json.client")
@RequestMapping(value = SearchEngineMappings.TYPE_MAPPING_FOR_LEGACY, produces = MediaType.APPLICATION_JSON_VALUE)
public interface ILegacySearchEngineJsonClient {

    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_ALL_MAPPING)
    ResponseEntity<JsonObject> searchAll(@RequestParam(required = false) MultiValueMap<String, String> allParams);

    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING)
    ResponseEntity<JsonObject> searchDataObjects(
            @RequestParam(required = false) MultiValueMap<String, String> allParams);

    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING)
    ResponseEntity<JsonObject> searchCollections(@RequestParam MultiValueMap<String, String> allParams);

    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING)
    ResponseEntity<JsonObject> searchDataobjectsReturnDatasets(@RequestParam MultiValueMap<String, String> allParams);

    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.SEARCH_DATASETS_MAPPING)
    ResponseEntity<JsonObject> searchDatasets(@RequestParam MultiValueMap<String, String> allParams);

    @RequestMapping(method = RequestMethod.GET, value = SearchEngineMappings.GET_DATAOBJECT_MAPPING)
    ResponseEntity<JsonObject> getDataobject(@Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn,
            @RequestHeader HttpHeaders headers);

}
