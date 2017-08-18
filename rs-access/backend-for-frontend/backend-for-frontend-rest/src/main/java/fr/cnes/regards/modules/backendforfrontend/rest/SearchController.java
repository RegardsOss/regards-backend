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
package fr.cnes.regards.modules.backendforfrontend.rest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.access.services.client.IServiceAggregatorClient;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.search.client.ICatalogClient;
import fr.cnes.regards.modules.search.client.ISearchAllClient;

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

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);

    /**
     * Function converting a {@link JsonArray} into a parallel {@link Stream}
     */
    private static final Function<JsonArray, Stream<JsonElement>> JSON_ARRAY_TO_STREAM = pJsonArray -> StreamSupport
            .stream(pJsonArray.spliterator(), true);

    @Autowired
    private ICatalogClient catalogClient;

    @Autowired
    private IServiceAggregatorClient serviceAggregatorClient;

    @Autowired
    private ISearchAllClient searchAllClient;

    @Autowired
    private Gson gson;

    /**
     * The main path
     */
    static final String ROOT_PATH = "";

    public static final String SEARCH = "/search";

    @RequestMapping(path = "/search/datazobjects", method = RequestMethod.GET)
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

    /**
     * Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of
     * collection, dataset, dataobject and document.
     *
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams
     *            all query parameters
     * @return the page of entities matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of collection, dataset, dataobject and document.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchAll(@RequestParam(required = false) final Map<String, String> allParams)
            throws SearchException {
        ResponseEntity<JsonObject> entities = searchAllClient.searchAll(allParams);
        injectApplicableServices(entities);
        return entities;
    }

    /**
     * Inject applicable Ui Services and Catalog Services into given entities
     *
     * @param pEntities The list of entities, represented as a {@link JsonObject} wrapped in a {@link ResponseEntity}
     */
    private void injectApplicableServices(ResponseEntity<JsonObject> pEntities) {
        try (Stream<JsonElement> elements = JSON_ARRAY_TO_STREAM
                .apply(pEntities.getBody().get("content").getAsJsonArray())) {
            // @formatter:off
            elements
                .map(JsonElement::getAsJsonObject)
                .map(element -> element.get("content"))
                .map(JsonElement::getAsJsonObject)
                .forEach(element -> element.add("services", entityToApplicableServices(element)));
            // @formatter:on
        }
    }

    /**
     * Returns the applicable services of the given entity
     *
     * @param pEntity The entity, represented as a {@link JsonObject}
     * @return The list of applicable services, represented as a {@link JsonObject}
     */
    private JsonElement entityToApplicableServices(JsonObject pEntity) {
        // @formatter:off
        List<Resource<PluginServiceDto>> applicableServices = JSON_ARRAY_TO_STREAM.apply(pEntity.get("tags").getAsJsonArray())
            .map(JsonElement::getAsString)
            .map(UniformResourceName::fromString)
            .filter(urn -> EntityType.DATASET.equals(urn.getEntityType()))
            .map(UniformResourceName::toString)
            .distinct()
            .map(datasetIpId -> serviceAggregatorClient.retrieveServices(datasetIpId, null))
            .map(ResponseEntity::getBody)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        // @formatter:on

        return gson.toJsonTree(applicableServices);
    }

}
