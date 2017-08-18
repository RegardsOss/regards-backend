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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestParamBuilder;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.access.services.client.IServiceAggregatorClient;
import fr.cnes.regards.modules.search.client.ISearchAllClient;

/**
 * Integration Test for {@link SearchController}
 *
 * @author Xavier-Alexandre Brochard
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class SearchControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SearchControllerIT.class);

    @Autowired
    private ISearchAllClient searchAllClient;

    @Autowired
    private IServiceAggregatorClient serviceAggregatorClient;

    @Test
    @Requirement("TODO")
    @Purpose("TODO")
    public void searchAll() {
        // Mock the ISearchAllClient
        Mockito.when(searchAllClient.searchAll(Mockito.any()))
                .thenReturn(BackendForFrontendTestUtils.SEARCH_ALL_RESULT);

        // Mock the IServiceAggregatorClient specifically for each dataset in order to check if they are properly injected
        Mockito.when(serviceAggregatorClient
                .retrieveServices(BackendForFrontendTestUtils.DATASET_0.getIpId().toString(), null))
                .thenReturn(BackendForFrontendTestUtils.SERVICES_FOR_DATASET_0);
        Mockito.when(serviceAggregatorClient
                .retrieveServices(BackendForFrontendTestUtils.DATASET_1.getIpId().toString(), null))
                .thenReturn(BackendForFrontendTestUtils.SERVICES_FOR_DATASET_1);

        // Define expectations
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content[0].content.services",
                                                        Matchers.hasSize(2)));
        expectations.add(MockMvcResultMatchers
                .jsonPath(JSON_PATH_ROOT + ".content[0].content.services[0].content.label", Matchers.equalTo("conf0")));
        expectations
                .add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content[0].content.services[1].content.label",
                                                    Matchers.equalTo("uiPluginConfiguration2")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".content[1].content.services",
                                                        Matchers.hasSize(1)));
        expectations.add(MockMvcResultMatchers
                .jsonPath(JSON_PATH_ROOT + ".content[1].content.services[0].content.label", Matchers.equalTo("conf1")));

        // Call
        RequestParamBuilder builder = RequestParamBuilder.build().param("q", "some:opensearchrequest");
        performDefaultGet(SearchController.ROOT_PATH + SearchController.SEARCH, expectations,
                          "Error searching all entities", builder);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
