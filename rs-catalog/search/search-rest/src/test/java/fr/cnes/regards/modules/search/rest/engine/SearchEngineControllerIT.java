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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.jayway.jsonpath.JsonPath;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.entities.domain.StaticProperties;
import fr.cnes.regards.modules.search.rest.SearchEngineController;

/**
 * Search engine tests
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(locations = { "classpath:test.properties" },
        properties = { "regards.tenant=legacy", "spring.jpa.properties.hibernate.default_schema=legacy" })
@MultitenantTransactional
public class SearchEngineControllerIT extends AbstractEngineIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineControllerIT.class);

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
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_ALL_MAPPING, customizer,
                          "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchCollections() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        addCommontMatchers(customizer);
        addSearchTermQuery(customizer, STAR, SUN);
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_COLLECTIONS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDatasets() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_DATASETS_MAPPING,
                          customizer, "Search all error", ENGINE_TYPE);
    }

    @Test
    public void searchDataobjects() {
        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.SEARCH_DATAOBJECTS_MAPPING,
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
        ResultActions result = performDefaultGet(SearchEngineController.TYPE_MAPPING
                + SearchEngineController.SEARCH_DATASETS_MAPPING, customizer, "Search all error", ENGINE_TYPE);

        String datasetUrn = JsonPath.read(payload(result), "$.content[0].content.ipId");

        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        addCommontMatchers(customizer);
        performDefaultGet(SearchEngineController.TYPE_MAPPING
                + SearchEngineController.SEARCH_DATASET_DATAOBJECTS_MAPPING, customizer, "Search all error",
                          ENGINE_TYPE, datasetUrn);
    }

    // TODO search document, dataobjects in dataset, dataobjects returning datasets

    /**
     * Add query to current request
     * @param customizer current {@link RequestBuilderCustomizer}
     * @param relativePropertyName name without properties prefix
     * @param value the property value
     */
    private void addSearchTermQuery(RequestBuilderCustomizer customizer, String relativePropertyName, String value) {
        customizer.customizeRequestParam()
                .param(SEARCH_TERMS_QUERY, StaticProperties.PROPERTIES + "." + relativePropertyName + ":" + value);
    }
}
