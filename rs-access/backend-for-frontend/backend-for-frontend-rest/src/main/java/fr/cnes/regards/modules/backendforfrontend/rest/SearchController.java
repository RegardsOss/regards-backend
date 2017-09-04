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
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.access.services.client.IServiceAggregatorClient;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.search.client.ISearchAllClient;
import fr.cnes.regards.modules.search.client.ISearchAllWithFacetsClient;
import fr.cnes.regards.modules.search.client.ISearchCollectionsClient;
import fr.cnes.regards.modules.search.client.ISearchDataobjectsClient;
import fr.cnes.regards.modules.search.client.ISearchDataobjectsReturnDatasetsClient;
import fr.cnes.regards.modules.search.client.ISearchDatasetsClient;
import fr.cnes.regards.modules.search.client.ISearchDocumentsClient;

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
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchController.class);

    /**
     * Function converting a {@link JsonArray} into a parallel {@link Stream}
     */
    private static final Function<JsonArray, Stream<JsonElement>> JSON_ARRAY_TO_STREAM = pJsonArray -> StreamSupport
            .stream(pJsonArray.spliterator(), false);

    @Autowired
    private IServiceAggregatorClient serviceAggregatorClient;

    @Autowired
    private ISearchAllClient searchAllClient;

    @Autowired
    private ISearchAllWithFacetsClient searchAllWithFacetsClient;

    @Autowired
    private ISearchCollectionsClient searchCollectionsClient;

    @Autowired
    private ISearchDatasetsClient searchDatasetsClient;

    @Autowired
    private ISearchDataobjectsClient searchDataobjectsClient;

    @Autowired
    private ISearchDataobjectsReturnDatasetsClient searchDataobjectsReturnDatasetsClient;

    @Autowired
    private ISearchDocumentsClient searchDocumentsClient;

    @Autowired
    private Gson gson;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * The main path
     */
    static final String ROOT_PATH = "";

    public static final String DATAOBJECTS_DATASETS_SEARCH = "/dataobjects/datasets/search";

    public static final String DOCUMENTS_SEARCH = "/documents/search";

    public static final String DATAOBJECTS_SEARCH = "/dataobjects/search";

    public static final String DATASETS_SEARCH = "/datasets/search";

    public static final String COLLECTIONS_SEARCH = "/collections/search";

    public static final String SEARCH_WITH_FACETS = "/searchwithfacets";

    public static final String SEARCH = "/search";

    /**
     * Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of
     * collection, dataset, dataobject and document.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams
     *            all query parameters
     * @return the search result with services injected
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of collection, dataset, dataobject and document. Injects applicable UI Services and Catalog Services.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchAll(@RequestParam(required = false) final Map<String, String> allParams)
            throws SearchException {
        ResponseEntity<JsonObject> entities = searchAllClient.searchAll(allParams);
        injectApplicableServices(entities);
        return entities;
    }

    /**
     * Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of
     * collection, dataset, dataobject and document. Allows usage of facets.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams
     *            all query parameters
     * @param pFacets
     *            facets to apply as a list of strings
     * @return the search result with services injected
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = SEARCH_WITH_FACETS, method = RequestMethod.GET)
    @ResourceAccess(role = DefaultRole.PUBLIC,
            description = "Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of collection, dataset, dataobject and document. Injects applicable UI Services and Catalog Services.")
    public ResponseEntity<JsonObject> searchAll(@RequestParam final Map<String, String> allParams,
            @RequestParam(value = "facets", required = false) final String[] pFacets) throws SearchException {
        ResponseEntity<JsonObject> entities = searchAllWithFacetsClient.searchAll(allParams, pFacets);
        injectApplicableServices(entities);
        return entities;
    }

    /**
     * Perform an OpenSearch request on collections.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams
     *            all query parameters
     * @return the search result with services injected
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = COLLECTIONS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on collections. Injects applicable UI Services and Catalog Services.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchCollections(@RequestParam final Map<String, String> allParams)
            throws SearchException {
        ResponseEntity<JsonObject> entities = searchCollectionsClient.searchCollections(allParams);
        injectApplicableServices(entities);
        return entities;
    }

    /**
     * Perform an OpenSearch request on datasets.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams
     *            all query parameters
     * @return the search result with services injected
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = DATASETS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on datasets. Injects applicable UI Services and Catalog Services.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchDatasets(@RequestParam final Map<String, String> allParams)
            throws SearchException {
        ResponseEntity<JsonObject> entities = searchDatasetsClient.searchDatasets(allParams);
        injectApplicableServices(entities);
        return entities;
    }

    /**
     * Perform an OpenSearch request on dataobjects. Only return required facets.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams
     *            all query parameters
     * @param pFacets
     *            the facets to apply
     * @return the search result with services injected
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = DATAOBJECTS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on dataobjects. Only return required facets. Injects applicable UI Services and Catalog Services.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchDataobjects(@RequestParam final Map<String, String> allParams,
            @RequestParam(value = "facets", required = false) String[] pFacets) throws SearchException {
        LOGGER.error("[XAB] We are on tenant " + runtimeTenantResolver.getTenant());
        ResponseEntity<JsonObject> entities = searchDataobjectsClient.searchDataobjects(allParams, pFacets);
        injectApplicableServices(entities);
        return entities;
    }

    /**
     * Perform an joined OpenSearch request. The search will be performed on dataobjects attributes, but will return the
     * associated datasets.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams
     *            all query parameters
     * @param pFacets
     *            the facets to apply
     * @return the search result with services injected
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = DATAOBJECTS_DATASETS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an joined OpenSearch request. The search will be performed on dataobjects attributes, but will return the associated datasets. Injects applicable UI Services and Catalog Services.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchDataobjectsReturnDatasets(@RequestParam final Map<String, String> allParams,
            @RequestParam(value = "facets", required = false) final String[] pFacets) throws SearchException {
        ResponseEntity<JsonObject> entities = searchDataobjectsReturnDatasetsClient
                .searchDataobjectsReturnDatasets(allParams, pFacets);
        injectApplicableServices(entities);
        return entities;
    }

    /**
     * Perform an OpenSearch request on documents.
     * <p>
     * Also injects the applicable Ui Services and Catalog Services.
     *
     * @param allParams
     *            all query parameters
     * @return the search result with services injected
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = DOCUMENTS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on documents. Injects applicable UI Services and Catalog Services.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<JsonObject> searchDocuments(@RequestParam final Map<String, String> allParams)
            throws SearchException {
        ResponseEntity<JsonObject> entities = searchDocumentsClient.searchDocuments(allParams);
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
        String tenant = runtimeTenantResolver.getTenant();

        // @formatter:off
        List<Resource<PluginServiceDto>> applicableServices = JSON_ARRAY_TO_STREAM.apply(pEntity.get("tags").getAsJsonArray())
            .map(JsonElement::getAsString)
            .map(UniformResourceName::fromString)
            .filter(urn -> EntityType.DATASET.equals(urn.getEntityType()))
            .map(UniformResourceName::toString)
            .distinct()
            .peek(unused -> runtimeTenantResolver.forceTenant(tenant))
//            .peek(unused -> FeignSecurityManager.asSystem()) // Enable system call
            .map(datasetIpId -> serviceAggregatorClient.retrieveServices(datasetIpId, null))
//            .peek(unused -> FeignSecurityManager.reset()) // Disable system call
            .peek(unused -> runtimeTenantResolver.clearTenant())
            .map(ResponseEntity::getBody)
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toList());
        // @formatter:on

        return gson.toJsonTree(applicableServices);
    }

}
