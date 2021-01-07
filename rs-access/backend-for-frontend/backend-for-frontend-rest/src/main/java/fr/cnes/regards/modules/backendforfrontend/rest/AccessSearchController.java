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
package fr.cnes.regards.modules.backendforfrontend.rest;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.access.services.client.IServiceAggregatorClient;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineJsonClient;

/**
 * Controller proxying rs-catalog's CatalogController in order to inject services.
 * @author Xavier-Alexandre Brochard
 */
@RestController
@RequestMapping(path = AccessSearchController.ROOT_PATH)
public class AccessSearchController {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessSearchController.class);

    /**
     * Function converting a {@link JsonArray} into a {@link Stream}
     */
    private static final Function<JsonArray, Stream<JsonElement>> JSON_ARRAY_TO_STREAM = pJsonArray -> StreamSupport
            .stream(pJsonArray.spliterator(), false);

    @Autowired
    private IServiceAggregatorClient serviceAggregatorClient;

    @Autowired
    private ILegacySearchEngineJsonClient searchClient;

    @Autowired
    private Gson gson;

    /**
     * The main path
     */
    static final String ROOT_PATH = "";

    public static final String DATAOBJECTS_DATASETS_SEARCH = "/dataobjects/datasets/search";

    public static final String DATAOBJECTS_SEARCH = "/dataobjects/search";

    public static final String DATASETS_SEARCH = "/datasets/search";

    public static final String COLLECTIONS_SEARCH = "/collections/search";

    public static final String SEARCH = "/search";

    /**
     * Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of
     * collection, dataset, dataobject and document.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     * @param allParams all query parameters
     * @return the search result with services injected
     */
    @RequestMapping(path = SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on all indexed data, regardless of the type. The return "
                    + "objects can be any mix of collection, dataset, dataobject and document. Injects applicable "
                    + "UI Services and Catalog Services.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchAll(
            @RequestParam(required = false) final MultiValueMap<String, String> allParams) {
        // before everything, we need to encode '+' and only '+' which is interpreted as ' ' by jetty
        // and which is not encoded by feign which is respecting RFC1738 on URI
        encodePlus(allParams);
        try {
            JsonObject entities = searchClient.searchAll(allParams).getBody();
            injectApplicableServices(entities);
            return new ResponseEntity<>(entities, HttpStatus.OK);
        } catch (HttpServerErrorException | HttpClientErrorException exception) {
            LOGGER.error(exception.getMessage(), exception);
            JsonObject jsonObject = new JsonParser().parse(exception.getResponseBodyAsString()).getAsJsonObject();
            HttpStatus statusCode = exception.getStatusCode();
            return new ResponseEntity<>(jsonObject, statusCode);
        }
    }

    /**
     * Perform an OpenSearch request on collections.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     * @param allParams all query parameters
     * @return the search result with services injected
     */
    @RequestMapping(path = COLLECTIONS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on collections. Injects applicable UI Services and Catalog "
                    + "Services.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchCollections(@RequestParam final MultiValueMap<String, String> allParams) {
        // before everything, we need to encode '+' and only '+' which is interpreted as ' ' by jetty
        // and which is not encoded by feign which is respecting RFC1738 on URI
        encodePlus(allParams);
        try {
            JsonObject entities = searchClient.searchCollections(allParams).getBody();
            injectApplicableServices(entities);
            return new ResponseEntity<>(entities, HttpStatus.OK);
        } catch (HttpServerErrorException | HttpClientErrorException exception) {
            LOGGER.error(exception.getMessage(), exception);
            JsonObject jsonObject = new JsonParser().parse(exception.getResponseBodyAsString()).getAsJsonObject();
            HttpStatus statusCode = exception.getStatusCode();
            return new ResponseEntity<>(jsonObject, statusCode);
        }
    }

    /**
     * Perform an OpenSearch request on datasets.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     * @param allParams all query parameters
     * @return the search result with services injected
     */
    @RequestMapping(path = DATASETS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on datasets. Injects applicable UI Services and Catalog "
                    + "Services.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchDatasets(@RequestParam final MultiValueMap<String, String> allParams) {
        // before everything, we need to encode '+' and only '+' which is interpreted as ' ' by jetty
        // and which is not encoded by feign which is respecting RFC1738 on URI
        encodePlus(allParams);
        try {
            JsonObject entities = searchClient.searchDatasets(allParams).getBody();
            injectApplicableServices(entities);
            return new ResponseEntity<>(entities, HttpStatus.OK);
        } catch (HttpServerErrorException | HttpClientErrorException exception) {
            LOGGER.error(exception.getMessage(), exception);
            JsonObject jsonObject = new JsonParser().parse(exception.getResponseBodyAsString()).getAsJsonObject();
            HttpStatus statusCode = exception.getStatusCode();
            return new ResponseEntity<>(jsonObject, statusCode);
        }
    }

    /**
     * Perform an OpenSearch request on dataobjects. Only return required facets.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     * @param allParams a MultiValueMap containing all request params (multi-valued because of multi "sort" params)
     * @return the search result with services injected
     */
    @RequestMapping(path = DATAOBJECTS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on dataobjects. Only return required facets. Injects "
            + "applicable UI Services and Catalog Services.", role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchDataobjects(@RequestParam MultiValueMap<String, String> allParams) {
        // before everything, we need to encode '+' and only '+' which is interpreted as ' ' by jetty
        // and which is not encoded by feign which is respecting RFC1738 on URI
        encodePlus(allParams);
        try {
            JsonObject entities = searchClient.searchDataObjects(allParams).getBody();
            injectApplicableServices(entities);
            return new ResponseEntity<>(entities, HttpStatus.OK);
        } catch (HttpServerErrorException | HttpClientErrorException exception) {
            LOGGER.error(exception.getMessage(), exception);
            JsonObject jsonObject = new JsonParser().parse(exception.getResponseBodyAsString()).getAsJsonObject();
            HttpStatus statusCode = exception.getStatusCode();
            return new ResponseEntity<>(jsonObject, statusCode);
        }
    }

    private void encodePlus(MultiValueMap<String, String> allParams) {
        allParams.forEach((param, values) -> values.replaceAll(value -> value.replaceAll("\\+", "%2B")));
    }

    /**
     * Perform a joined OpenSearch request. The search will be performed on dataobjects attributes, but will return the
     * associated datasets.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     * @param allParams all query parameters
     * @return the search result with services injected
     */
    @RequestMapping(path = DATAOBJECTS_DATASETS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an joined OpenSearch request. The search will be performed on dataobjects attributes,"
                    + " but will return the associated datasets. Injects applicable UI Services and Catalog Services.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchDataobjectsReturnDatasets(
            @RequestParam final MultiValueMap<String, String> allParams) {
        // before everything, we need to encode '+' and only '+' which is interpreted as ' ' by jetty
        // and which is not encoded by feign which is respecting RFC1738 on URI
        encodePlus(allParams);
        try {
            JsonObject entities = searchClient.searchDataobjectsReturnDatasets(allParams).getBody();
            injectApplicableServices(entities);
            return new ResponseEntity<>(entities, HttpStatus.OK);
        } catch (HttpServerErrorException | HttpClientErrorException exception) {
            LOGGER.error(exception.getMessage(), exception);
            JsonObject jsonObject = new JsonParser().parse(exception.getResponseBodyAsString()).getAsJsonObject();
            HttpStatus statusCode = exception.getStatusCode();
            return new ResponseEntity<>(jsonObject, statusCode);
        }
    }

    /**
     * Inject applicable Ui Services and Catalog Services into given entities
     * @param pEntities The list of entities, represented as a {@link JsonObject} wrapped in a {@link ResponseEntity}
     */
    private void injectApplicableServices(JsonObject pEntities) {
        try (Stream<JsonElement> elements = JSON_ARRAY_TO_STREAM.apply(pEntities.get("content").getAsJsonArray())) {
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
     * @param pEntity The entity, represented as a {@link JsonObject}
     * @return The list of applicable services, represented as a {@link JsonObject}
     */
    private JsonElement entityToApplicableServices(JsonObject pEntity) {
        // @formatter:off
        List<EntityModel<PluginServiceDto>> applicableServices = JSON_ARRAY_TO_STREAM.apply(pEntity.get("tags").getAsJsonArray()) // Retrieve tags list and convert it to stream
            .filter(jsonElement -> !jsonElement.isJsonNull())
            .map(JsonElement::getAsString) // Convert elements of the stream to strings
            .filter(UniformResourceName::isValidUrn) // Only keep URNs
            .map(UniformResourceName::fromString) // Convert elements of the stream to URNs
            .filter(urn -> EntityType.DATASET.equals(urn.getEntityType())) // Only keep URNs of datasets
            .map(UniformResourceName::toString) // Go back to strings
            .distinct() // Remove doubles
            .map(datasetIpId -> serviceAggregatorClient.retrieveServices(Arrays.asList(datasetIpId), null))
            .map(ResponseEntity::getBody)
            .flatMap(List::stream) // Now each element of the stream is a List of services, so we flatten the structure in a stream of services
            .distinct() // Remove doubles
            .collect(Collectors.toList());
        // @formatter:on

        return gson.toJsonTree(applicableServices);
    }

}
