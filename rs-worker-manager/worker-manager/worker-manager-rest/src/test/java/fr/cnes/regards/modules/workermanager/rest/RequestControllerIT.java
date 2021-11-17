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
package fr.cnes.regards.modules.workermanager.rest;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.workermanager.dao.IRequestRepository;
import fr.cnes.regards.modules.workermanager.domain.request.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * {@link Request} REST API testing
 *
 * @author Théo Lasserre
 *
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=request_controller_it" })
@ContextConfiguration(classes = { RequestControllerIT.Config.class })
public class RequestControllerIT extends AbstractRegardsIT {

    @Autowired
    private IRequestRepository requestRepository;

    @Configuration
    static class Config {

    }

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        createRequests("requestId", OffsetDateTime.now(), "contentType", "source1", "session1", RequestStatus.DISPATCHED, "".getBytes(), "error", 1);
    }

    @After
    public void cleanUp() {
        // Clean everything
        requestRepository.deleteAll();
    }

    private void createRequests(String requestId, OffsetDateTime creationDate, String contentType, String source, String session, RequestStatus status, byte[] content, String error, int nbRequests) {
        List<Request> requests = Lists.newArrayList();
        for (int i = 0; i < nbRequests; i++) {
            Request request = new Request();
            request.setRequestId(requestId + i);
            request.setCreationDate(creationDate);
            request.setContentType(contentType);
            request.setSource(source);
            request.setSession(session);
            request.setStatus(status);
            request.setContent(content);
            request.setError(error);
            requests.add(request);
        }
        requests = requestRepository.saveAll(requests);
        Assert.assertEquals(nbRequests, requests.size());
    }

    @Test
    public void retrieveAllRequestList() {
        // Retrieve without filters
        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expectStatusOk();
        requestBuilderCustomizer.expectIsArray(JSON_PATH_CONTENT);
        requestBuilderCustomizer.expectToHaveSize(JSON_PATH_CONTENT, 1);
        SearchRequestParameters body = new SearchRequestParameters();
        body.withStatusesIncluded(RequestStatus.DISPATCHED);
        performDefaultPost(RequestController.TYPE_MAPPING, body, requestBuilderCustomizer, "Error retrieving requests");
    }

    @Test
    public void retrieveARequest() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expectStatusOk();
        performDefaultGet(RequestController.TYPE_MAPPING + RequestController.REQUEST_ID_PATH, requestBuilderCustomizer, "Error retrieving a request", "requestId0");
    }

    @Test
    public  void retryRequests() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expectStatusOk();
        SearchRequestParameters body = new SearchRequestParameters();
        performDefaultPost(RequestController.TYPE_MAPPING + RequestController.REQUEST_RETRY_PATH, body, requestBuilderCustomizer, "Error retry requests");
    }

    @Test
    public  void deleteRequests() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.expectStatusOk();
        SearchRequestParameters body = new SearchRequestParameters();
        performDefaultDelete(RequestController.TYPE_MAPPING + RequestController.REQUEST_DELETE_PATH, body, requestBuilderCustomizer, "Error delete requests");
    }
}
