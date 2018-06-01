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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.search.rest.SearchEngineController;

/**
 * TODO
 * @author Marc Sordi
 *
 */
// @TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=engines",
// "regards.elasticsearch.address=@regards.IT.elasticsearch.host@", "regards.elasticsearch.cluster.name=regards",
// "regards.elasticsearch.tcp.port=@regards.IT.elasticsearch.port@" })
@TestPropertySource(locations = { "classpath:test.properties" })
public class SearchEngineControllerIT extends AbstractRegardsTransactionalIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineControllerIT.class);

    private static final String DATASET_ID = "datatest";

    private static final String ENGINE_TYPE = "opensearch";

    @Test
    public void searchAll() {

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());

        // customizer.customizeHeaders().setContentType(MediaType.APPLICATION_ATOM_XML);
        // customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_XML));
        customizer.customizeHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_ATOM_XML));

        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        customizer.customizeRequestParam().param("facets", "toto", "titi");
        performDefaultGet(SearchEngineController.TYPE_MAPPING, customizer, "Search all error", ENGINE_TYPE);

        // RequestParamBuilder builder = RequestParamBuilder.build().param("q", CatalogControllerTestUtils.Q);
        // performDefaultGet(SearchEngineController.TYPE_MAPPING, expectations, "Error searching", DATASET_ID,
        // ENGINE_TYPE);
    }

    @Test
    public void basicExtraSearch() {
        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());

        // RequestParamBuilder builder = RequestParamBuilder.build().param("q", CatalogControllerTestUtils.Q);
        // performDefaultGet(SearchEngineController.TYPE_MAPPING + SearchEngineController.EXTRA_MAPPING, expectations,
        // "Error searching", DATASET_ID, ENGINE_TYPE, "opensearchdescription.xml");
    }
}
