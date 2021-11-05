package fr.cnes.regards.modules.workermanager.rest;

import java.time.OffsetDateTime;
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.workermanager.dao.IRequestRepository;
import fr.cnes.regards.modules.workermanager.domain.dto.requests.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;

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
        performDefaultPost(RequestController.TYPE_MAPPING + RequestController.REQUEST_DELETE_PATH, body, requestBuilderCustomizer, "Error delete requests");
    }
}
