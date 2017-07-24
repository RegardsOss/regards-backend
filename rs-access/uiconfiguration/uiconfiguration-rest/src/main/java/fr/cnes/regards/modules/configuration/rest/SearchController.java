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
package fr.cnes.regards.modules.configuration.rest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Controller proxying rs-catalog's CatalogController in order to inject services.
 *
 * @author Xavier-Alexandre Brochard
 */
@RestController
@ModuleInfo(name = "search", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(path = SearchController.ROOT_PATH)
public class SearchController {

    @Autowired
    private ICatalogClient catalogClient;

    /**
     * The main path
     */
    static final String ROOT_PATH = "/search";

    @RequestMapping(path = "/datazobjects", method = RequestMethod.GET)
    @ResourceAccess(description = "Search datazobjects", role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchDataobjects(
            @RequestParam(required = false) final Map<String, String> allParams) throws SearchException {
        ResponseEntity<JsonObject> dataobjects = catalogClient.searchDataobjects(allParams);

        // Add a propery in json for each element
        JsonArray elements = dataobjects.getBody().get("content").getAsJsonArray();
        for (JsonElement jsonElement : elements) {
            jsonElement.getAsJsonObject().get("content").getAsJsonObject().addProperty("proxyAddedProperty", "hey");
        }

        return dataobjects;
    }

}
