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
package fr.cnes.regards.modules.search.rest.engine;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.jayway.jsonpath.JsonPath;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;

/**
 * Search engine tests
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(locations = { "classpath:test.properties" },
        properties = { "regards.tenant=legacy", "spring.jpa.properties.hibernate.default_schema=legacy" })
@MultitenantTransactional
public class LegacySearchEngineControllerIT extends AbstractEngineIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacySearchEngineControllerIT.class);

    private static final String ENGINE_TYPE = "legacy";

    private static final String SEARCH_TERMS_QUERY = "q";

    private void addCommontMatchers(RequestBuilderCustomizer customizer) {
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.links", Matchers.not(Matchers.emptyArray())));
    }

    @Test
    public void searchAll() {

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        addCommontMatchers(customizer);
        // customizer.customizeHeaders().setContentType(MediaType.APPLICATION_ATOM_XML);
        // customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_XML));
        // customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_ATOM_XML));
        // customizer.customizeRequestParam().param("facets", "toto", "titi");

        // customizer.customizeRequestParam().param("page", "0");
        // customizer.customizeRequestParam().param("size", "2");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_ALL_MAPPING, customizer,
                          "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchCollections() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(1)));
        addCommontMatchers(customizer);
        addSearchTermQuery(customizer, STAR, SUN);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchCollectionsWithShortName() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(1)));
        addCommontMatchers(customizer);
        customizer.customizeRequestParam().param(SEARCH_TERMS_QUERY, STAR + ":" + SUN);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchCollectionPropertyValues() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.customizeRequestParam().param("maxCount", "10");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_PROPERTY_VALUES,
                          customizer, "Search all error", ENGINE_TYPE,
                          StaticProperties.FEATURE_PROPERTIES + "." + GALAXY);
    }

    @Test
    public void searchDatasets() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASETS_MAPPING, customizer,
                          "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataobjects() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    // http://172.26.47.52/api/v1/rs-access-project/dataobjects/search?
    // sort=properties.fragment1.activated,ASC&sort=label,ASC
    // &facets=properties.description,properties.fragment1.activated&offset=0&page=0&size=500

    @Test
    public void searchDataobjectsWithFacets() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        addCommontMatchers(customizer);
        // Sort
        customizer.customizeRequestParam().param("sort", PLANET_TYPE + ",ASC");
        customizer.customizeRequestParam().param("sort", PLANET + ",ASC");
        // Facets
        customizer.customizeRequestParam().param("facets", PLANET_TYPE);

        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataobjectsFilteredByDatasetModels() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(1)));
        // Filter
        customizer.customizeRequestParam().param(SEARCH_TERMS_QUERY, StaticProperties.DATASET_MODEL_IDS + ":(99)");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);

        // No match
        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(0)));
        customizer.customizeRequestParam().param(SEARCH_TERMS_QUERY, StaticProperties.DATASET_MODEL_IDS + ":(999)");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataobjectPropertyValues() {

        // Search the 9 planets
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.length()", Matchers.equalTo(9)));

        customizer.customizeRequestParam().param("maxCount", "10");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTY_VALUES,
                          customizer, "Search all error", ENGINE_TYPE,
                          StaticProperties.FEATURE_PROPERTIES + "." + PLANET);

        // Search only the 8 planets of the solar system
        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.length()", Matchers.equalTo(8)));

        // Retrieve dataset URN
        Dataset solarSystem = getAstroObject(SOLAR_SYSTEM);
        Assert.assertNotNull(solarSystem);

        customizer.customizeRequestParam().param("maxCount", "10");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING
                + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_PROPERTY_VALUES, customizer, "Search all error",
                          ENGINE_TYPE, solarSystem.getIpId().toString(),
                          StaticProperties.FEATURE_PROPERTIES + "." + PLANET);
    }

    @Test
    public void searchDataobjectPropertyValuesFilteredByDatasetModels() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.length()", Matchers.equalTo(1)));

        // Filter
        customizer.customizeRequestParam().param(SEARCH_TERMS_QUERY, StaticProperties.DATASET_MODEL_IDS + ":(99)");

        customizer.customizeRequestParam().param("maxCount", "100");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTY_VALUES,
                          customizer, "Search all error", ENGINE_TYPE, StaticProperties.FEATURE_MODEL);
    }

    @Test
    public void searchDataobjectPropertyBounds() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.length()", Matchers.equalTo(3)));
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$..[?(@.propertyName=='properties.diameter')]")
                .exists());
        customizer.addExpectation(MockMvcResultMatchers
                .jsonPath("$..[?(@.propertyName=='properties.diameter')].lowerBound", Matchers.hasItem(1000.0)));
        customizer.addExpectation(MockMvcResultMatchers
                .jsonPath("$..[?(@.propertyName=='properties.diameter')].upperBound", Matchers.hasItem(143000.0)));

        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$..[?(@.propertyName=='properties.sun_distance')]")
                .exists());
        customizer.addExpectation(MockMvcResultMatchers
                .jsonPath("$..[?(@.propertyName=='properties.sun_distance')].lowerBound",
                          Matchers.hasItem(4721979696256909312L)));
        customizer.addExpectation(MockMvcResultMatchers
                .jsonPath("$..[?(@.propertyName=='properties.sun_distance')].upperBound",
                          Matchers.hasItem(4751501522070667264L)));

        customizer.addExpectation(MockMvcResultMatchers
                .jsonPath("$..[?(@.propertyName=='properties.TimePeriod.startDate')]").exists());

        customizer.customizeRequestParam().param("properties", "diameter", "sun_distance", "TimePeriod.startDate",
                                                 "unknown.attribute");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTIES_BOUNDS,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataobjectsInDataset() {

        // Search dataset
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        addCommontMatchers(customizer);
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(1)));
        addSearchTermQuery(customizer, STAR_SYSTEM, SOLAR_SYSTEM);
        ResultActions result = performDefaultGet(SearchEngineMappings.TYPE_MAPPING
                + SearchEngineMappings.SEARCH_DATASETS_MAPPING, customizer, "Search all error", ENGINE_TYPE);

        String datasetUrn = JsonPath.read(payload(result), "$.content[0].content.id");

        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        addCommontMatchers(customizer);
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.links.length()", Matchers.equalTo(1)));
        customizer.addExpectation(MockMvcResultMatchers.jsonPath("$.content[0].links.length()", Matchers.equalTo(1)));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE, datasetUrn);
    }

    // TODO search document, dataobjects returning datasets

    /**
     * Add query to current request
     * @param customizer current {@link RequestBuilderCustomizer}
     * @param relativePropertyName name without properties prefix
     * @param value the property value
     */
    private void addSearchTermQuery(RequestBuilderCustomizer customizer, String relativePropertyName, String value) {
        customizer.customizeRequestParam()
                .param(SEARCH_TERMS_QUERY,
                       StaticProperties.FEATURE_PROPERTIES + "." + relativePropertyName + ":" + value);
    }
}
