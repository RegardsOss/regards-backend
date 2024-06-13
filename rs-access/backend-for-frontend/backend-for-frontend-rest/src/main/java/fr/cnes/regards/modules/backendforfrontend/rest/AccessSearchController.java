/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.access.services.client.cache.CacheableServiceAggregatorClient;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.search.client.ILegacySearchEngineJsonClient;
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

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Controller proxying rs-catalog's CatalogController in order to inject services.
 *
 * @author Xavier-Alexandre Brochard
 */
@RestController
@RequestMapping(path = AccessSearchController.ROOT_PATH)
public class AccessSearchController {

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
     * Function converting a {@link JsonArray} into a {@link Stream}
     */
    private static final Function<JsonArray, Stream<JsonElement>> JSON_ARRAY_TO_STREAM = pJsonArray -> StreamSupport.stream(
        pJsonArray.spliterator(),
        false);

    private CacheableServiceAggregatorClient serviceAggregatorClient;

    private ILegacySearchEngineJsonClient searchClient;

    private Gson gson;

    public AccessSearchController(CacheableServiceAggregatorClient serviceAggregatorClient,
                                  ILegacySearchEngineJsonClient searchClient,
                                  Gson gson) {
        this.serviceAggregatorClient = serviceAggregatorClient;
        this.searchClient = searchClient;
        this.gson = gson;
    }

    /**
     * Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of
     * collection, dataset, dataobject and document.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams all query parameters
     * @return the search result with services injected
     */
    @RequestMapping(path = SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description =
                        "Perform an OpenSearch request on all indexed data, regardless of the type. The return "
                        + "objects can be any mix of collection, dataset, dataobject and document. Injects applicable "
                        + "UI Services and Catalog Services.", role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchAll(@RequestParam(required = false) MultiValueMap<String, String> allParams)
        throws HttpClientErrorException, HttpServerErrorException {
        JsonObject entities = searchClient.searchAll(allParams).getBody();
        injectApplicableServices(entities);
        return new ResponseEntity<>(entities, HttpStatus.OK);

    }

    /**
     * Perform an OpenSearch request on collections.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams all query parameters
     * @return the search result with services injected
     */
    @RequestMapping(path = COLLECTIONS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description =
                        "Perform an OpenSearch request on collections. Injects applicable UI Services and Catalog "
                        + "Services.", role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchCollections(@RequestParam MultiValueMap<String, String> allParams)
        throws HttpClientErrorException, HttpServerErrorException {
        JsonObject entities = searchClient.searchCollections(allParams).getBody();
        injectApplicableServices(entities);
        return new ResponseEntity<>(entities, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on datasets.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams all query parameters
     * @return the search result with services injected
     */
    @RequestMapping(path = DATASETS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description =
                        "Perform an OpenSearch request on datasets. Injects applicable UI Services and Catalog "
                        + "Services.", role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchDatasets(@RequestParam MultiValueMap<String, String> allParams)
        throws HttpClientErrorException, HttpServerErrorException {
        JsonObject entities = searchClient.searchDatasets(allParams).getBody();
        injectApplicableServices(entities);
        return new ResponseEntity<>(entities, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on dataobjects. Only return required facets.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams a MultiValueMap containing all request params (multi-valued because of multi "sort" params)
     * @return the search result with services injected
     */
    @RequestMapping(path = DATAOBJECTS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on dataobjects. Only return required facets. Injects "
                                  + "applicable UI Services and Catalog Services.", role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchDataobjects(@RequestParam MultiValueMap<String, String> allParams)
        throws HttpClientErrorException, HttpServerErrorException {
        JsonObject entities = searchClient.searchDataObjects(allParams).getBody();
        injectApplicableServices(entities);
        return new ResponseEntity<>(entities, HttpStatus.OK);
    }

    /**
     * Perform a joined OpenSearch request. The search will be performed on dataobjects attributes, but will return the
     * associated datasets.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams all query parameters
     * @return the search result with services injected
     */
    @RequestMapping(path = DATAOBJECTS_DATASETS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description =
                        "Perform an joined OpenSearch request. The search will be performed on dataobjects attributes,"
                        + " but will return the associated datasets. Injects applicable UI Services and Catalog Services.",
                    role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchDataobjectsReturnDatasets(
        @RequestParam MultiValueMap<String, String> allParams)
        throws HttpClientErrorException, HttpServerErrorException {
        JsonObject entities = searchClient.searchDataobjectsReturnDatasets(allParams).getBody();
        injectApplicableServices(entities);
        return new ResponseEntity<>(entities, HttpStatus.OK);
    }

    /**
     * Inject applicable Ui Services and Catalog Services into given entities
     *
     * @param entities The list of entities, represented as a {@link JsonObject} wrapped in a {@link ResponseEntity}
     */
    private void injectApplicableServices(JsonObject entities) {
        if (entities != null) {
            try (Stream<JsonElement> elements = JSON_ARRAY_TO_STREAM.apply(entities.get("content").getAsJsonArray())) {
                elements.map(JsonElement::getAsJsonObject)
                        .map(element -> element.get("content"))
                        .map(JsonElement::getAsJsonObject)
                        .forEach(element -> element.add("services", entityToApplicableServices(element)));
            }
        } else {
            throw new RsRuntimeException("An error occurred while injecting application services: pEntities is null");
        }
    }

    /**
     * Returns the applicable services of the given entity
     *
     * @param pEntity The entity, represented as a {@link JsonObject}
     * @return The list of applicable services, represented as a {@link JsonObject}
     */
    private JsonElement entityToApplicableServices(JsonObject pEntity) {
        List<EntityModel<PluginServiceDto>> applicableServices = JSON_ARRAY_TO_STREAM.apply(pEntity.get("tags")
                                                                                                   .getAsJsonArray()) // Retrieve tags list and convert it to stream
                                                                                     .filter(jsonElement -> !jsonElement.isJsonNull())
                                                                                     .map(JsonElement::getAsString) // Convert elements of the stream to strings
                                                                                     .filter(UniformResourceName::isValidUrn) // Only keep URNs
                                                                                     .map(UniformResourceName::fromString) // Convert elements of the stream to URNs
                                                                                     .filter(urn -> EntityType.DATASET.equals(
                                                                                         urn.getEntityType())) // Only keep URNs of datasets
                                                                                     .map(UniformResourceName::toString) // Go back to strings
                                                                                     .distinct() // Remove doubles
                                                                                     .map(datasetIpId -> serviceAggregatorClient.retrieveServices(
                                                                                         Arrays.asList(datasetIpId),
                                                                                         null))
                                                                                     .map(ResponseEntity::getBody)
                                                                                     .flatMap(List::stream) // Now each element of the stream is a List of services, so we flatten the structure in a stream of services
                                                                                     .distinct() // Remove doubles
                                                                                     .collect(Collectors.toList());

        return gson.toJsonTree(applicableServices);
    }

}
