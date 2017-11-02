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

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.gson.JsonObject;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import static fr.cnes.regards.modules.search.client.IJsonSearchClient.PATH;

/**
 * Feign client to call SearchController methods but with JsonObject as result types.
 * This client is mostly called by rs-access AccessSearchController (to avoid directly call SearchController from
 * the front)
 * @author oroussel
 */
@RestClient(name = "rs-catalog")
@RequestMapping(value = PATH, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IJsonSearchClient {
    String PATH = "/search";

    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<JsonObject> searchAll(@RequestParam(required = false) final Map<String, String> allParams);

    @RequestMapping(value = "/withfacets", method = RequestMethod.GET)
    ResponseEntity<JsonObject> searchAll(@RequestParam final Map<String, String> allParams,
            @RequestParam(value = "facets", required = false) final String[] pFacets);

    @RequestMapping(value = "/collections", method = RequestMethod.GET)
    ResponseEntity<JsonObject> searchCollections(@RequestParam final Map<String, String> allParams);

    @RequestMapping(value = "/dataobjects/withfacets", method = RequestMethod.GET)
    ResponseEntity<JsonObject> searchDataobjects(@RequestParam final Map<String, String> allParams,
            @RequestParam(value = "facets", required = false) String[] pFacets);

    @RequestMapping(value = "/dataobjects/datasets", method = RequestMethod.GET)
    ResponseEntity<JsonObject> searchDataobjectsReturnDatasets(@RequestParam final Map<String, String> allParams,
            @RequestParam(value = "facets", required = false) final String[] pFacets);

    @RequestMapping(value = "/datasets", method = RequestMethod.GET)
    ResponseEntity<JsonObject> searchDatasets(@RequestParam final Map<String, String> allParams);

    @RequestMapping(value = "/documents", method = RequestMethod.GET)
    ResponseEntity<JsonObject> searchDocuments(@RequestParam final Map<String, String> allParams);
}
