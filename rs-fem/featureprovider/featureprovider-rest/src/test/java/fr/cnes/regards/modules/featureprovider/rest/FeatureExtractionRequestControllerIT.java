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
package fr.cnes.regards.modules.featureprovider.rest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationMetadataEntity;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.FeatureRequestsSelectionDTO;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.featureprovider.dao.IFeatureExtractionRequestRepository;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequest;

/**
 *
 * Test class to validate endpoints of {@link FeatureExtractionRequestController}
 *
 * @author Sébastien Binda
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_requests",
        "spring.jpa.properties.hibernate.jdbc.batch_size=1024" })
@ActiveProfiles(value = { "noscheduler" })
public class FeatureExtractionRequestControllerIT extends AbstractRegardsIT {

    @Autowired
    private IFeatureExtractionRequestRepository extractionRequestRepo;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    OffsetDateTime date;

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        // Delete existing requests
        extractionRequestRepo.deleteAll();
        // Creates new ones
        createRequests("source1", "session1", 100, RequestState.GRANTED);
        createRequests("source1", "session2", 50, RequestState.GRANTED);
        createRequests("source1", "session2", 30, RequestState.ERROR);
        date = OffsetDateTime.now();
        createRequests("source1", "session3", 20, RequestState.ERROR);
    }

    @Test
    public void retrieveRequests() {
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());

        // Retrieve without filters
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$.content")
                .expectToHaveSize("$.content", 200).expectValue("$.info.nbErrors", 50);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        performDefaultGet(FeatureExtractionRequestController.ROOT_PATH
                + FeatureExtractionRequestController.REQUEST_SEARCH_TYPE_PATH, requestBuilderCustomizer,
                          "Error retrieving extraction requests");

        // Retrieve by state
        requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$.content")
                .expectToHaveSize("$.content", 50).expectValue("$.info.nbErrors", 50);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("state", RequestState.ERROR.toString());
        performDefaultGet(FeatureExtractionRequestController.ROOT_PATH
                + FeatureExtractionRequestController.REQUEST_SEARCH_TYPE_PATH, requestBuilderCustomizer,
                          "Error retrieving extraction requests");

        // Retrieve by date
        requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$.content")
                .expectToHaveSize("$.content", 20).expectValue("$.info.nbErrors", 20);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("from", date.toString());
        performDefaultGet(FeatureExtractionRequestController.ROOT_PATH
                + FeatureExtractionRequestController.REQUEST_SEARCH_TYPE_PATH, requestBuilderCustomizer,
                          "Error retrieving extraction requests");

        // Retrieve by source and session
        requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$.content")
                .expectToHaveSize("$.content", 100).expectValue("$.info.nbErrors", 0);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("source", "source1");
        requestBuilderCustomizer.addParameter("session", "session1");
        performDefaultGet(FeatureExtractionRequestController.ROOT_PATH
                + FeatureExtractionRequestController.REQUEST_SEARCH_TYPE_PATH, requestBuilderCustomizer,
                          "Error retrieving extraction requests");

        requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$.content")
                .expectToHaveSize("$.content", 80).expectValue("$.info.nbErrors", 30);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("source", "source1");
        requestBuilderCustomizer.addParameter("session", "session2");
        performDefaultGet(FeatureExtractionRequestController.ROOT_PATH
                + FeatureExtractionRequestController.REQUEST_SEARCH_TYPE_PATH, requestBuilderCustomizer,
                          "Error retrieving extraction requests");

    }

    @Test
    public void deleteRequests() {
        // Create 10 requests scheduled, so they cannot be deleted
        createRequests("delete_source", "test1", 10, RequestState.GRANTED);
        // Create 10 requests in error status, so theu can be deleted
        createRequests("delete_source", "test1", 10, RequestState.ERROR);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                .expectValue("$.totalHandled", 10).expectValue("$.totalRequested", 10);
        FeatureRequestsSelectionDTO selection = FeatureRequestsSelectionDTO.build().withSource("delete_source");
        performDefaultDelete(FeatureExtractionRequestController.ROOT_PATH, selection, requestBuilderCustomizer,
                             "Error deleting requests");

        // Now all error feature of source retry_source should be deleted
        requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$.content")
                .expectToHaveSize("$.content", 10).expectValue("$.info.nbErrors", 0);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("source", "delete_source");
        performDefaultGet(FeatureExtractionRequestController.ROOT_PATH
                + FeatureExtractionRequestController.REQUEST_SEARCH_TYPE_PATH, requestBuilderCustomizer,
                          "Invlid number of requests from source retry_source");

    }

    @Test
    public void retryRequests() {
        // Create 10 requests scheduled, so they cannot be deleted
        createRequests("retry_source", "test1", 10, RequestState.GRANTED);
        // Create 10 requests in error status, so theu can be deleted
        createRequests("retry_source", "test1", 10, RequestState.ERROR);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                .expectValue("$.totalHandled", 10).expectValue("$.totalRequested", 10);
        FeatureRequestsSelectionDTO selection = FeatureRequestsSelectionDTO.build().withSource("retry_source");
        performDefaultPost(FeatureExtractionRequestController.ROOT_PATH + FeatureExtractionRequestController.RETRY_PATH,
                           selection, requestBuilderCustomizer, "Error retrying requests");

        // Now all error feature of source retry_source should be deleted
        requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$.content")
                .expectToHaveSize("$.content", 20).expectValue("$.info.nbErrors", 0);
        requestBuilderCustomizer.addParameter("page", "0");
        requestBuilderCustomizer.addParameter("size", "1000");
        requestBuilderCustomizer.addParameter("source", "retry_source");
        performDefaultGet(FeatureExtractionRequestController.ROOT_PATH
                + FeatureExtractionRequestController.REQUEST_SEARCH_TYPE_PATH, requestBuilderCustomizer,
                          "Invlid number of requests from source retry_source");

    }

    private void createRequests(String source, String session, int nbRequests, RequestState state) {
        List<FeatureExtractionRequest> requests = Lists.newArrayList();
        for (int i = 0; i < nbRequests; i++) {
            FeatureCreationMetadataEntity metadata = FeatureCreationMetadataEntity.build(source, session,
                                                                                         Lists.newArrayList(), true);
            requests.add(FeatureExtractionRequest.build(UUID.randomUUID().toString(), "owner", OffsetDateTime.now(),
                                                        state, metadata, FeatureRequestStep.LOCAL_DELAYED,
                                                        PriorityLevel.NORMAL, new JsonObject(), "factory"));
        }
        requests = extractionRequestRepo.saveAll(requests);
        Assert.assertEquals(nbRequests, requests.size());
    }
}
