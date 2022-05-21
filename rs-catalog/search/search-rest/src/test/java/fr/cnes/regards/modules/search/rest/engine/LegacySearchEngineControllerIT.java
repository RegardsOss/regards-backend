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
package fr.cnes.regards.modules.search.rest.engine;

import com.jayway.jsonpath.JsonPath;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.dam.domain.entities.criterion.IFeatureCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.search.domain.plugin.SearchEngineMappings;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

/**
 * Search engine tests
 *
 * @author Marc Sordi
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
        customizer.expectIsNotEmpty("$.links");
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
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_ALL_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void searchCollections() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expectValue("$.content.length()", 1);
        addCommontMatchers(customizer);
        addSearchTermQuery(customizer, STAR, SUN);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void searchCollectionsWithRegexp() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expectValue("$.content.length()", 1);
        addCommontMatchers(customizer);
        addSearchTermQuery(customizer, STAR, "/S[^i]{2}/");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void fullTextSearchCollections() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expectValue("$.content.length()", 1);
        addCommontMatchers(customizer);
        addFullTextSearchQuery(customizer, SUN);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void searchCollectionsWithShortName() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expectValue("$.content.length()", 1);
        addCommontMatchers(customizer);
        customizer.addParameter(SEARCH_TERMS_QUERY, STAR + ":" + SUN);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void searchCollectionsWithShortNameAndInFullTextSearch() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expectValue("$.content.length()", 1);
        addCommontMatchers(customizer);
        customizer.addParameter(SEARCH_TERMS_QUERY, fullTextMatching(STAR) + ":" + SUN.toLowerCase());
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void searchCollectionPropertyValues() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.addParameter("maxCount", "10");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_COLLECTIONS_PROPERTY_VALUES,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE,
                          StaticProperties.FEATURE_PROPERTIES + "." + GALAXY);
    }

    @Test
    public void searchDatasets() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASETS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void searchDataset() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        performDefaultGet(
            SearchEngineMappings.TYPE_MAPPING_FOR_LEGACY + "/datasets/" + solarSystem.getIpId().toString(),
            customizer,
            "Search dataset error",
            ENGINE_TYPE);
    }

    @Test
    @Purpose("Test if the sorting is properly done on datasets returned by the ES search when there is no search "
        + "criteria")
    public void searchDataobjectsReturnDatasetsWithoutSearchCriterion() {
        // Sort on STRING
        RequestBuilderCustomizer customizerOnString1 = customizer().expectStatusOk();
        customizerOnString1.addParameter("page", "0");
        customizerOnString1.addParameter("size", "2");
        customizerOnString1.addParameter("sort", String.format("%s,DESC", StaticProperties.FEATURE_LABEL));
        addCommontMatchers(customizerOnString1);
        customizerOnString1.expectValue("$.content[0].content.providerId", SOLAR_SYSTEM);
        customizerOnString1.expectValue("$.content[1].content.providerId", PEGASI_51);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING,
                          customizerOnString1,
                          "Search all error",
                          ENGINE_TYPE);
        RequestBuilderCustomizer customizerOnString2 = customizer().expectStatusOk();
        customizerOnString2.addParameter("page", "1");
        customizerOnString2.addParameter("size", "2");
        customizerOnString2.addParameter("sort", String.format("%s,DESC", StaticProperties.FEATURE_LABEL));
        addCommontMatchers(customizerOnString2);
        customizerOnString2.expectValue("$.content[0].content.providerId", KEPLER_90);
        customizerOnString2.expectValue("$.content[1].content.providerId", KEPLER_16);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING,
                          customizerOnString2,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    @Purpose("Test if the sorting is properly done on datasets returned by the ES search when there is search criteria")
    public void searchDataobjectsReturnDatasetsWithSearchCriterion() {
        // Sort on DATE
        RequestBuilderCustomizer customizerOnDate = customizer().expectStatusOk();
        customizerOnDate.addParameter(SEARCH_TERMS_QUERY,
                                      String.format("%s:%s", PLANET_TYPE, protect(PLANET_TYPE_GAS_GIANT)));
        customizerOnDate.addParameter("page", "0");
        customizerOnDate.addParameter("sort", String.format("properties.%s,DESC", STUDY_DATE));
        customizerOnDate.addParameter("sort", String.format("%s,ASC", StaticProperties.FEATURE_LABEL));
        addCommontMatchers(customizerOnDate);
        customizerOnDate.expectValue("$.content[0].content.providerId", SOLAR_SYSTEM);
        customizerOnDate.expectValue("$.content[1].content.providerId", KEPLER_16);
        customizerOnDate.expectValue("$.content[2].content.providerId", PEGASI_51);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING,
                          customizerOnDate,
                          "Search all error",
                          ENGINE_TYPE);

        // Sort on INT
        RequestBuilderCustomizer customizerOnInt = customizer().expectStatusOk();
        customizerOnInt.addParameter(SEARCH_TERMS_QUERY, String.format("%s:[35000 TO *]", PLANET_DIAMETER));
        customizerOnInt.addParameter("page", "0");
        customizerOnInt.addParameter("sort", String.format("properties.%s,ASC", NUMBER_OF_PLANETS));
        customizerOnInt.addParameter("sort", String.format("properties.%s,ASC", RESEARCH_LAB));
        customizerOnInt.addParameter("sort", String.format("%s,ASC", StaticProperties.FEATURE_LABEL));
        addCommontMatchers(customizerOnInt);
        customizerOnInt.expectValue("$.content[0].content.providerId", KEPLER_16);
        customizerOnInt.expectValue("$.content[1].content.providerId", PEGASI_51);
        customizerOnInt.expectValue("$.content[2].content.providerId", SOLAR_SYSTEM);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING,
                          customizerOnInt,
                          "Search all error",
                          ENGINE_TYPE);

        // Sort on DOUBLE
        RequestBuilderCustomizer customizerOnDouble1 = customizer().expectStatusOk();
        customizerOnDouble1.addParameter(SEARCH_TERMS_QUERY, String.format("%s:[* TO 50000]", PLANET_DIAMETER));
        customizerOnDouble1.addParameter("page", "0");
        customizerOnDouble1.addParameter("size", "2");
        customizerOnDouble1.addParameter("sort", String.format("properties.%s,DESC", DISTANCE_TO_SOLAR_SYSTEM));
        addCommontMatchers(customizerOnDouble1);
        customizerOnDouble1.expectValue("$.content[0].content.providerId", KEPLER_90);
        customizerOnDouble1.expectValue("$.content[1].content.providerId", SOLAR_SYSTEM);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING,
                          customizerOnDouble1,
                          "Search all error",
                          ENGINE_TYPE);

        // Sort on URL with two pages
        RequestBuilderCustomizer customizerOnUrl1 = customizer().expectStatusOk();
        customizerOnUrl1.addParameter(SEARCH_TERMS_QUERY, String.format("%s:%d", StaticProperties.FEATURE_VERSION, 1));
        customizerOnUrl1.addParameter("page", "0");
        customizerOnUrl1.addParameter("size", "2");
        customizerOnUrl1.addParameter("sort", String.format("properties.%s,DESC", RESEARCH_LAB));
        customizerOnUrl1.addParameter("sort", String.format("%s,DESC", StaticProperties.FEATURE_LABEL));
        addCommontMatchers(customizerOnUrl1);
        customizerOnUrl1.expectValue("$.content[0].content.providerId", KEPLER_90);
        customizerOnUrl1.expectValue("$.content[1].content.providerId", SOLAR_SYSTEM);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING,
                          customizerOnUrl1,
                          "Search all error",
                          ENGINE_TYPE);

        RequestBuilderCustomizer customizerOnUrl2 = customizer().expectStatusOk();
        customizerOnUrl2.addParameter(SEARCH_TERMS_QUERY, String.format("%s:%d", StaticProperties.FEATURE_VERSION, 1));
        customizerOnUrl2.addParameter("page", "1");
        customizerOnUrl2.addParameter("size", "2");
        customizerOnUrl2.addParameter("sort", String.format("properties.%s,DESC", RESEARCH_LAB));
        customizerOnUrl2.addParameter("sort", String.format("%s,DESC", StaticProperties.FEATURE_LABEL));
        addCommontMatchers(customizerOnUrl2);
        customizerOnUrl2.expectValue("$.content[0].content.providerId", PEGASI_51);
        customizerOnUrl2.expectValue("$.content[1].content.providerId", KEPLER_16);

        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_DATASETS_MAPPING,
                          customizerOnUrl2,
                          "Search all error",
                          ENGINE_TYPE);

    }

    @Test
    public void searchDataObjects() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void searchDataObjectsByRawdata() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expectValue("$.metadata.totalElements", 1);
        customizer.addParameter(SEARCH_TERMS_QUERY, StaticProperties.FEATURE_FILE_RAWDATA_FILENAME + ":(Mercury.txt)");
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                customizer, "Search all error", ENGINE_TYPE);

        customizer = customizer().expectStatusOk();
        customizer.expectValue("$.metadata.totalElements", 12);
        customizer.addParameter(SEARCH_TERMS_QUERY, StaticProperties.FEATURE_FILE_RAWDATA_FILENAME + ":(*.txt)");
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                customizer, "Search all error", ENGINE_TYPE);

        customizer = customizer().expectStatusOk();
        customizer.expectValue("$.metadata.totalElements", 0);
        customizer.addParameter(SEARCH_TERMS_QUERY, StaticProperties.FEATURE_FILE_RAWDATA_FILENAME + ":(Mercury.unknown)");
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataObjectsAttributes() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expectToHaveSize("$", 2);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_ATTRIBUTES,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void fullTextSearchDataOjects() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        customizer.addParameter("sort", "providerId" + ",ASC");
        customizer.expectValue("$.content[0].content.providerId", JUPITER);
        customizer.expectValue("$.content[1].content.providerId", MERCURY);
        addFullTextSearchQuery(customizer, "\"" + MERCURY + " " + JUPITER + "\"");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void searchDataObjectsOnJsonAttribute() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        customizer.addParameter("sort", "providerId" + ",ASC");
        customizer.expectValue("$.content.length()", 0);
        addSearchTermQuery(customizer, "origine.name", "ESA");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);

        customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        customizer.addParameter("sort", "providerId" + ",ASC");
        customizer.expectValue("$.content.length()", 12);
        addSearchTermQuery(customizer, "origine.name", "CNE*");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void fullTextSearchDataObjectsWithWildcards() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        customizer.addParameter("sort", "providerId" + ",ASC");
        customizer.expectValue("$.content[0].content.providerId", JUPITER);
        customizer.expectValue("$.content[1].content.providerId", MERCURY);
        addFullTextSearchQuery(customizer, JUPITER.substring(0, 3) + "* OR " + MERCURY.substring(0, 4) + "*");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void fullTextSearchDataObjectsIntoStringArray() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        customizer.expectValue("$.content[0].content.providerId", JUPITER);
        addFullTextSearchQuery(customizer, ALPHA_PARAM);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void searchDataObjectsWithFacets() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        // Sort
        customizer.addParameter("sort", PLANET_TYPE + ",ASC");
        customizer.addParameter("sort", PLANET + ",ASC");
        // Facets
        customizer.addParameter("facets", PLANET_TYPE);

        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void searchDataObjectsFilteredByDatasetModels() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expectValue("$.content.length()", 1);
        // Filter
        customizer.addParameter(SEARCH_TERMS_QUERY, StaticProperties.DATASET_MODEL_NAMES + ":(planet)");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);

        // No match
        customizer = customizer().expectStatusOk();
        customizer.expectValue("$.content.length()", 0);
        customizer.addParameter(SEARCH_TERMS_QUERY, StaticProperties.DATASET_MODEL_NAMES + ":(exoplanet)");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void searchDataObjectPropertyValues() {

        // Search the 9 planets
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expectValue("$.length()", 12);

        customizer.addParameter("maxCount", "13");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTY_VALUES,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE,
                          StaticProperties.FEATURE_PROPERTIES + "." + PLANET);

        // Search only the 8 planets of the solar system
        customizer = customizer().expectStatusOk();
        customizer.expectValue("$.length()", 8);

        // Retrieve dataset URN
        Dataset solarSystem = getAstroObject(SOLAR_SYSTEM);
        Assert.assertNotNull(solarSystem);

        customizer.addParameter("maxCount", "10");
        performDefaultGet(
            SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_PROPERTY_VALUES,
            customizer,
            "Search all error",
            ENGINE_TYPE,
            solarSystem.getIpId().toString(),
            StaticProperties.FEATURE_PROPERTIES + "." + PLANET);
    }

    @Test
    public void searchDataObjectPropertyValuesFilteredByDatasetModels() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expectValue("$.length()", 1);

        // Filter
        customizer.addParameter(SEARCH_TERMS_QUERY, StaticProperties.DATASET_MODEL_NAMES + ":(planet)");

        customizer.addParameter("maxCount", "100");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTY_VALUES,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE,
                          StaticProperties.FEATURE_MODEL);
    }

    @Test
    public void searchDataObjectPropertyBounds() {
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        customizer.expectValue("$.length()", 3);
        customizer.expectIsNotEmpty("$..[?(@.propertyName=='properties.diameter')]");
        customizer.expectValue("$..[?(@.propertyName=='properties.diameter')].lowerBound", 1000);
        customizer.expectValue("$..[?(@.propertyName=='properties.diameter')].upperBound", 8000000);

        customizer.expectIsNotEmpty("$..[?(@.propertyName=='properties.sun_distance')]");
        customizer.expectValue("$..[?(@.propertyName=='properties.sun_distance')].lowerBound", 7000000);
        customizer.expectValue("$..[?(@.propertyName=='properties.sun_distance')].upperBound", 4_489_435_980L);

        customizer.expectIsNotEmpty("$..[?(@.propertyName=='properties.TimePeriod.startDate')]");

        customizer.addParameter("properties", "diameter", "sun_distance", "TimePeriod.startDate", "unknown.attribute");
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATAOBJECTS_PROPERTIES_BOUNDS,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE);
    }

    @Test
    public void searchDataObjectsInDataset() {

        // Search dataset
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        customizer.expectValue("$.content.length()", 1);
        addSearchTermQuery(customizer, STAR_SYSTEM, protect(SOLAR_SYSTEM));
        ResultActions result = performDefaultGet(
            SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASETS_MAPPING,
            customizer,
            "Search all error",
            ENGINE_TYPE);

        String datasetUrn = JsonPath.read(payload(result), "$.content[0].content.id");

        customizer = customizer().expectStatusOk();
        addCommontMatchers(customizer);
        customizer.expectValue("$.links.length()", 1);
        customizer.expectValue("$.content[0].links.length()", 1);
        performDefaultGet(SearchEngineMappings.TYPE_MAPPING + SearchEngineMappings.SEARCH_DATASET_DATAOBJECTS_MAPPING,
                          customizer,
                          "Search all error",
                          ENGINE_TYPE,
                          datasetUrn);
    }

    /**
     * Add query to current request
     *
     * @param customizer           current {@link RequestBuilderCustomizer}
     * @param relativePropertyName name without properties prefix
     * @param value                the property value
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

    private String fullTextMatching(String property) {
        return property + IFeatureCriterion.STRING_MATCH_TYPE_SEPARATOR
            + StringMatchType.FULL_TEXT_SEARCH.getMatchTypeValue();
    }

    private String keywordMatching(String property) {
        return property + IFeatureCriterion.STRING_MATCH_TYPE_SEPARATOR + StringMatchType.KEYWORD.getMatchTypeValue();
    }
}
