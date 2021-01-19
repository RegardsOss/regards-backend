/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
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

    @Autowired(required = false)
    private List<HealthIndicator> indicators;

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacySearchEngineControllerIT.class);

    private static final String ENGINE_TYPE = "legacy";

    private static final String SEARCH_TERMS_QUERY = "q";

    @Test
    public void healthTest() {
        if (indicators != null) {
            for (HealthIndicator indicator : indicators) {
                Health health = indicator.health();
                LOGGER.debug("{} : {}", indicator.getClass(), health.getStatus());
            }
        }
    }

    private void addCommontMatchers(RequestBuilderCustomizer customizer) {
        customizer.expect(MockMvcResultMatchers.jsonPath("$.links", Matchers.not(Matchers.emptyArray())));
    }

    @Test
    public void searchAll() {

        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        // customizer.customizeHeaders().setContentType(MediaType.APPLICATION_ATOM_XML);
        // customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_XML));
        // customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_ATOM_XML));
        // customizer.addParameter("facets", "toto", "titi");

        // customizer.addParameter("page", "0");
        // customizer.addParameter("size", "2");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_ALL_MAPPING, customizer,
                          "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchCollections() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(1)));
        addCommontMatchers(customizer);
        addSearchTermQuery(customizer, STAR, SUN);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void fullTextSearchCollections() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(1)));
        addCommontMatchers(customizer);
        addFullTextSearchQuery(customizer, SUN);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchCollectionsWithShortName() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(1)));
        addCommontMatchers(customizer);
        customizer.addParameter(SEARCH_TERMS_QUERY, STAR + ":" + SUN);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchCollectionPropertyValues() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.addParameter("maxCount", "10");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_PROPERTY_VALUES,
                          customizer, "Search all error", ENGINE_TYPE,
                          StaticProperties.FEATURE_PROPERTIES + "." + GALAXY);
    }

    @Test
    public void searchDatasets() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASETS_MAPPING, customizer,
                          "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataset() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING_FOR_LEGACY + "/datasets/"
                + solarSystem.getIpId().toString(), customizer, "Search dataset error", ENGINE_TYPE);
    }

    @Test
    public void searchDataobjects() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataobjectsAttributes() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(2)));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_ATTRIBUTES,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void fullTextSearchDataobjects() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        customizer.addParameter("sort", "providerId" + ",ASC");
        customizer.expectValue("$.content[0].content.providerId", JUPITER);
        customizer.expectValue("$.content[1].content.providerId", MERCURY);
        addFullTextSearchQuery(customizer, "\"" + MERCURY + " " + JUPITER + "\"");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void fullTextSearchDataobjectsWithWildcards() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        customizer.addParameter("sort", "providerId" + ",ASC");
        customizer.expectValue("$.content[0].content.providerId", JUPITER);
        customizer.expectValue("$.content[1].content.providerId", MERCURY);
        addFullTextSearchQuery(customizer, JUPITER.substring(0, 3) + "* OR " + MERCURY.substring(0, 4) + "*");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void fullTextSearchDataobjectsIntoStringArray() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        customizer.expectValue("$.content[0].content.providerId", JUPITER);
        addFullTextSearchQuery(customizer, ALPHA_PARAM);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataobjectsWithFacets() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        // Sort
        customizer.addParameter("sort", PLANET_TYPE + ",ASC");
        customizer.addParameter("sort", PLANET + ",ASC");
        // Facets
        customizer.addParameter("facets", PLANET_TYPE);

        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataobjectsFilteredByDatasetModels() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(1)));
        // Filter
        customizer.addParameter(SEARCH_TERMS_QUERY, StaticProperties.DATASET_MODEL_NAMES + ":(planet)");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);

        // No match
        customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(0)));
        customizer.addParameter(SEARCH_TERMS_QUERY, StaticProperties.DATASET_MODEL_NAMES + ":(exoplanet)");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataobjectPropertyValues() {

        // Search the 9 planets
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.equalTo(9)));

        customizer.addParameter("maxCount", "10");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTY_VALUES,
                          customizer, "Search all error", ENGINE_TYPE,
                          StaticProperties.FEATURE_PROPERTIES + "." + PLANET);

        // Search only the 8 planets of the solar system
        customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.equalTo(8)));

        // Retrieve dataset URN
        Dataset solarSystem = getAstroObject(SOLAR_SYSTEM);
        Assert.assertNotNull(solarSystem);

        customizer.addParameter("maxCount", "10");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING
                + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_PROPERTY_VALUES, customizer, "Search all error",
                          ENGINE_TYPE, solarSystem.getIpId().toString(),
                          StaticProperties.FEATURE_PROPERTIES + "." + PLANET);
    }

    @Test
    public void searchDataobjectPropertyValuesFilteredByDatasetModels() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.equalTo(1)));

        // Filter
        customizer.addParameter(SEARCH_TERMS_QUERY, StaticProperties.DATASET_MODEL_NAMES + ":(planet)");

        customizer.addParameter("maxCount", "100");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTY_VALUES,
                          customizer, "Search all error", ENGINE_TYPE, StaticProperties.FEATURE_MODEL);
    }

    @Test
    public void searchDataobjectPropertyBounds() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expect(MockMvcResultMatchers.jsonPath("$.length()", Matchers.equalTo(3)));
        customizer.expect(MockMvcResultMatchers.jsonPath("$..[?(@.propertyName=='properties.diameter')]").exists());
        customizer.expect(MockMvcResultMatchers.jsonPath("$..[?(@.propertyName=='properties.diameter')].lowerBound",
                                                         Matchers.hasItem(1000)));
        customizer.expect(MockMvcResultMatchers.jsonPath("$..[?(@.propertyName=='properties.diameter')].upperBound",
                                                         Matchers.hasItem(143000)));

        customizer.expect(MockMvcResultMatchers.jsonPath("$..[?(@.propertyName=='properties.sun_distance')]").exists());
        customizer.expect(MockMvcResultMatchers.jsonPath("$..[?(@.propertyName=='properties.sun_distance')].lowerBound",
                                                         Matchers.hasItem(50000000)));
        customizer.expect(MockMvcResultMatchers.jsonPath("$..[?(@.propertyName=='properties.sun_distance')].upperBound",
                                                         Matchers.hasItem(4_489_435_980L)));

        customizer.expect(MockMvcResultMatchers.jsonPath("$..[?(@.propertyName=='properties.TimePeriod.startDate')]")
                .exists());

        customizer.addParameter("properties", "diameter", "sun_distance", "TimePeriod.startDate", "unknown.attribute");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTIES_BOUNDS,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataobjectsInDataset() {

        // Search dataset
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content.length()", Matchers.equalTo(1)));
        addSearchTermQuery(customizer, STAR_SYSTEM, protect(SOLAR_SYSTEM));
        ResultActions result = performDefaultGet(SearchEngineMappings.TYPE_MAPPING
                + SearchEngineMappings.SEARCH_DATASETS_MAPPING, customizer, "Search all error", ENGINE_TYPE);

        String datasetUrn = JsonPath.read(payload(result), "$.content[0].content.id");

        customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        customizer.expect(MockMvcResultMatchers.jsonPath("$.links.length()", Matchers.equalTo(1)));
        customizer.expect(MockMvcResultMatchers.jsonPath("$.content[0].links.length()", Matchers.equalTo(1)));
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE, datasetUrn);
    }

    /**
     * Add query to current request
     * @param customizer current {@link RequestBuilderCustomizer}
     * @param relativePropertyName name without properties prefix
     * @param value the property value
     */
    private void addSearchTermQuery(RequestBuilderCustomizer customizer, String relativePropertyName, String value) {
        customizer.addParameter(SEARCH_TERMS_QUERY,
                                StaticProperties.FEATURE_PROPERTIES + "." + relativePropertyName + ":" + value);
    }

    /**
     * Add full text query to current request
     */
    private void addFullTextSearchQuery(RequestBuilderCustomizer customizer, String value) {
        customizer.addParameter(SEARCH_TERMS_QUERY, value);
    }
}
