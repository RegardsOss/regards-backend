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
package fr.cnes.regards.modules.search.domain.plugin;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * Mappings for search engine controller and client
 *
 * @author Marc Sordi
 */
public final class SearchEngineMappings {

    /**
     * Search main namespace
     */
    public static final String TYPE_MAPPING = "/engines/{engineType}";

    public static final String LEGACY_PLUGIN_ID = "legacy";

    public static final String TYPE_MAPPING_FOR_LEGACY = "/engines/" + LEGACY_PLUGIN_ID;

    /**
     * Search route mapping
     */
    private static final String SEARCH_MAPPING = "/search";

    /**
     * Additional route mapping
     */
    private static final String EXTRA_MAPPING = "/{extra}";

    /**
     * To retrieve a single entity
     */
    private static final String URN_MAPPING = "/{urn}";

    /**
     * For entities with description
     */
    private static final String DESCRIPTION_MAPPING = "/description";

    /**
     * To get all values of a property
     */
    private static final String PROPERTY_VALUES_MAPPING = "/properties/{propertyName}/values";

    /**
     * To get properties bounds
     */
    private static final String PROPERTIES_BOUNDS_MAPPING = "/properties/bounds";

    /**
     * To get attributes associated to entities search results
     */
    private static final String ATTRIBUTES_MAPPING = "/attributes";

    // Search on all entities

    private static final String ENTITIES_MAPPING = "/entities";

    public static final String SEARCH_ALL_MAPPING = ENTITIES_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_ALL_MAPPING_EXTRA = SEARCH_ALL_MAPPING + EXTRA_MAPPING;

    public static final String GET_ENTITY_MAPPING = ENTITIES_MAPPING + URN_MAPPING;

    // Collection mappings

    private static final String COLLECTIONS_MAPPING = "/collections";

    public static final String SEARCH_COLLECTIONS_MAPPING = COLLECTIONS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_COLLECTIONS_MAPPING_EXTRA = SEARCH_COLLECTIONS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_COLLECTIONS_PROPERTY_VALUES = SEARCH_COLLECTIONS_MAPPING
                                                                    + PROPERTY_VALUES_MAPPING;

    public static final String GET_COLLECTION_MAPPING = COLLECTIONS_MAPPING + URN_MAPPING;

    // Dataset mapping

    private static final String DATASETS_MAPPING = "/datasets";

    public static final String SEARCH_DATASETS_MAPPING = DATASETS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_DATASETS_MAPPING_EXTRA = SEARCH_DATASETS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_DATASETS_PROPERTY_VALUES = SEARCH_DATASETS_MAPPING + PROPERTY_VALUES_MAPPING;

    public static final String GET_DATASET_MAPPING = DATASETS_MAPPING + URN_MAPPING;

    public static final String GET_DATASET_DESCRIPTION_MAPPING = GET_DATASET_MAPPING + DESCRIPTION_MAPPING;

    // Dataobject mapping

    private static final String DATAOBJECTS_MAPPING = "/dataobjects";

    public static final String SEARCH_DATAOBJECTS_MAPPING = DATAOBJECTS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_DATAOBJECTS_MAPPING_EXTRA = SEARCH_DATAOBJECTS_MAPPING + EXTRA_MAPPING;

    public static final String SEARCH_DATAOBJECTS_PROPERTY_VALUES = SEARCH_DATAOBJECTS_MAPPING
                                                                    + PROPERTY_VALUES_MAPPING;

    public static final String SEARCH_DATAOBJECTS_PROPERTIES_BOUNDS = SEARCH_DATAOBJECTS_MAPPING
                                                                      + PROPERTIES_BOUNDS_MAPPING;

    public static final String SEARCH_DATAOBJECTS_ATTRIBUTES = SEARCH_DATAOBJECTS_MAPPING + ATTRIBUTES_MAPPING;

    public static final String GET_DATAOBJECT_MAPPING = DATAOBJECTS_MAPPING + URN_MAPPING;

    // Search dataobjects on a single dataset mapping

    private static final String DATASET_DATAOBJECTS_MAPPING = "/datasets/{datasetUrn}/dataobjects";

    public static final String SEARCH_DATASET_DATAOBJECTS_MAPPING = DATASET_DATAOBJECTS_MAPPING + SEARCH_MAPPING;

    public static final String SEARCH_DATASET_DATAOBJECTS_MAPPING_EXTRA = SEARCH_DATASET_DATAOBJECTS_MAPPING
                                                                          + EXTRA_MAPPING;

    public static final String SEARCH_DATASET_DATAOBJECTS_PROPERTY_VALUES = SEARCH_DATASET_DATAOBJECTS_MAPPING
                                                                            + PROPERTY_VALUES_MAPPING;

    // Fallback to {@link #GET_DATAOBJECT_MAPPING} for single data object retrieval

    // Search on dataobjects returning datasets

    public static final String SEARCH_DATAOBJECTS_DATASETS_MAPPING = "/dataobjects/datasets" + SEARCH_MAPPING;

    // Parameter names

    public static final String PAGE = "page";

    public static final String SIZE = "size";

    public static final String ENGINE_TYPE = "engineType";

    public static final String EXTRA = "extra";

    public static final String URN = "urn";

    public static final String PROPERTY_NAME = "propertyName";

    public static final String PROPERTY_NAMES = "properties";

    public static final String DATASET_URN = "datasetUrn";

    public static final String MAX_COUNT = "maxCount";

    public static final String FILE_TYPES = "fileTypes";

    public static final String SEARCH_REQUEST_PARSER = "parser";

    private SearchEngineMappings() {
        // Nothing to do
    }

    public static HttpHeaders getJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
