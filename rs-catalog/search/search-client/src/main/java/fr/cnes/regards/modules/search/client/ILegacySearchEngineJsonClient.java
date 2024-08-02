/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import static fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Feign client to call SearchController methods but with JsonObject as result types.
 * This client is mostly called by rs-access AccessSearchController (to avoid calling directly SearchController from
 * the front)
 *
 * @author oroussel
 * @author Sébastien Binda
 */
@RestClient(name = "rs-catalog", contextId = "rs-catalog.legacy-search-engind-json.client")
public interface ILegacySearchEngineJsonClient {

    String ROOT_PATH = TYPE_MAPPING_FOR_LEGACY;

    @GetMapping(path = ROOT_PATH + SEARCH_ALL_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<JsonObject> searchAll(@SpringQueryMap MultiValueMap<String, String> allParams);

    @GetMapping(path = ROOT_PATH + SEARCH_DATAOBJECTS_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<JsonObject> searchDataObjects(@SpringQueryMap MultiValueMap<String, String> allParams);

    @GetMapping(path = ROOT_PATH + SEARCH_COLLECTIONS_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<JsonObject> searchCollections(@SpringQueryMap MultiValueMap<String, String> allParams);

    @GetMapping(path = ROOT_PATH + SEARCH_DATAOBJECTS_DATASETS_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<JsonObject> searchDataobjectsReturnDatasets(@SpringQueryMap MultiValueMap<String, String> allParams);

    @GetMapping(path = ROOT_PATH + SEARCH_DATASETS_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<JsonObject> searchDatasets(@SpringQueryMap MultiValueMap<String, String> allParams);

    @GetMapping(path = ROOT_PATH + GET_DATAOBJECT_MAPPING, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<JsonObject> getDataobject(@Valid @PathVariable(SearchEngineMappings.URN) UniformResourceName urn,
                                             @RequestHeader HttpHeaders headers);

}
