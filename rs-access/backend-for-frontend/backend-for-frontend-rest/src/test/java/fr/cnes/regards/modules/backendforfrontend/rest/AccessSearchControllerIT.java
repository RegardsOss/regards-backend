/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.hamcrest.Matchers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Integration Test for {@link AccessSearchController}
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=bff", "spring.datasource.type=" })
public class AccessSearchControllerIT extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccessSearchControllerIT.class);

    @Test
    @Requirement("REGARDS_DSL_ACC_USE_700")
    @Purpose("Check the system can inject applicable services to the result of a search")
    public void searchAll() {
        performDefaultGet(AccessSearchController.ROOT_PATH + AccessSearchController.SEARCH, customizer()
                .expectStatusOk().expectToHaveSize(JSON_PATH_ROOT + ".content[0].content.services", 2)
                .expectToHaveSize(JSON_PATH_ROOT + ".content[1].content.services", 1)
                .expectValue(JSON_PATH_ROOT + ".content[0].content.services[0].content.label", "conf0")
                .expectValue(JSON_PATH_ROOT + ".content[0].content.services[1].content.label", "uiPluginConfiguration2")
                .expectValue(JSON_PATH_ROOT + ".content[1].content.services[0].content.label", "conf1")
                .addParameter("q", BackendForFrontendTestUtils.OPENSEARCH_QUERY), "Error searching all entities");
    }

    @Test
    @Requirement("REGARDS_DSL_ACC_USE_700")
    @Purpose("Check the system can inject applicable services to the result of a search")
    public void searchCollections() {
        performDefaultGet(AccessSearchController.ROOT_PATH + AccessSearchController.COLLECTIONS_SEARCH,
                          customizer().expectStatusOk()
                                  .expectToHaveSize(JSON_PATH_ROOT + ".content[0].content.services", 1)
                                  .expectValue(JSON_PATH_ROOT + ".content[0].content.services[0].content.label",
                                               "conf1")
                                  .addParameter("q", BackendForFrontendTestUtils.OPENSEARCH_QUERY),
                          "Error searching collections");
    }

    @Test
    @Requirement("REGARDS_DSL_ACC_USE_700")
    @Purpose("Check the system can inject applicable services to the result of a search")
    public void searchDatasets() {
        performDefaultGet(AccessSearchController.ROOT_PATH + AccessSearchController.DATASETS_SEARCH, customizer()
                .expectStatusOk().expectToHaveSize(JSON_PATH_ROOT + ".content[0].content.services", 0)
                .expectToHaveSize(JSON_PATH_ROOT + ".content[1].content.services", 2)
                .expectValue(JSON_PATH_ROOT + ".content[1].content.services[0].content.label", "conf0")
                .expectValue(JSON_PATH_ROOT + ".content[1].content.services[1].content.label", "uiPluginConfiguration2")
                .addParameter("q", BackendForFrontendTestUtils.OPENSEARCH_QUERY), "Error searching datasets");
    }

    @Test
    @Requirement("REGARDS_DSL_ACC_USE_700")
    @Purpose("Check the system can inject applicable services to the result of a search")
    public void searchDataobjects() {
        performDefaultGet(AccessSearchController.ROOT_PATH + AccessSearchController.DATAOBJECTS_SEARCH, customizer()
                .expectStatusOk().expectToHaveSize(JSON_PATH_ROOT + ".content[0].content.services", 2)
                .expectValue(JSON_PATH_ROOT + ".content[0].content.services[0].content.label", "conf0")
                .expectValue(JSON_PATH_ROOT + ".content[0].content.services[1].content.label", "uiPluginConfiguration2")
                .addParameter("q", BackendForFrontendTestUtils.OPENSEARCH_QUERY), "Error searching datasets");
    }

    @Test
    @Requirement("REGARDS_DSL_ACC_USE_700")
    @Purpose("Check the system can inject applicable services to the result of a search")
    public void searchDocuments() {
        performDefaultGet(AccessSearchController.ROOT_PATH + AccessSearchController.DOCUMENTS_SEARCH, customizer()
                .expectStatusOk().expectToHaveSize(JSON_PATH_ROOT + ".content[0].content.services", 3)
                .expect(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content[0].content.services[*].content.label",
                                                       Matchers.containsInAnyOrder("conf0", "conf1",
                                                                                   "uiPluginConfiguration2")))
                .addParameter("q", BackendForFrontendTestUtils.OPENSEARCH_QUERY), "Error searching datasets");
    }

    @Test
    @Requirement("REGARDS_DSL_ACC_USE_700")
    @Purpose("Check the system can inject applicable services to the result of a search")
    public void searchDataobjectsReturnDatasets() {
        // Define customizer
        RequestBuilderCustomizer customizer = customizer().expectStatusOk()
                .expectToHaveSize(JSON_PATH_ROOT + ".content[0].content.services", 0)
                .expectToHaveSize(JSON_PATH_ROOT + ".content[1].content.services", 2)
                .expectValue(JSON_PATH_ROOT + ".content[1].content.services[0].content.label", "conf0")
                .expectValue(JSON_PATH_ROOT + ".content[1].content.services[1].content.label", "uiPluginConfiguration2")
                .addParameter("q", BackendForFrontendTestUtils.OPENSEARCH_QUERY);
        // Call
        performDefaultGet(AccessSearchController.ROOT_PATH + AccessSearchController.DATAOBJECTS_DATASETS_SEARCH,
                          customizer, "Error searching datasets");
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
