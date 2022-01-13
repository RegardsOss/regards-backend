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

package fr.cnes.regards.modules.workermanager.service.flow;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.workermanager.dao.IRequestRepository;
import fr.cnes.regards.modules.workermanager.dao.IWorkerConfigRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerManagerSettings;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.domain.request.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.dto.WorkerConfigDto;
import fr.cnes.regards.modules.workermanager.dto.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.dto.events.RawMessageBuilder;
import fr.cnes.regards.modules.workermanager.dto.events.out.ResponseStatus;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.config.WorkerConfigService;
import fr.cnes.regards.modules.workermanager.service.requests.scan.RequestScanService;
import fr.cnes.regards.modules.workermanager.service.sessions.SessionHelper;
import fr.cnes.regards.modules.workermanager.task.NoWorkerAvailableScanRequestTaskScheduler;
import org.bouncycastle.cert.ocsp.Req;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@ActiveProfiles(value = { "default", "test", "testAmqp" }, inheritProfiles = false)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=worker_manager_it",
        "regards.amqp.enabled=true","regards.workermanager.request.bulk.size=1000","regards.amqp.enabled=true" },
        locations = { "classpath:application-test.properties" })
@ContextConfiguration(classes = { RequestHandlerConfiguration.class } )
public class RequestHandlerTest extends AbstractWorkerManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandlerTest.class);

    @Autowired
    private DynamicTenantSettingService tenantSettingService;

    @Autowired
    private RequestScanService requestScanService;

    @Autowired
    private WorkerConfigService workerConfigService;

    @Autowired
    private IWorkerConfigRepository workerConfigRepo;

    private final static String CONTENT_TYPE_TO_SKIP = "toskip";

    @Before
    public void init() throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        tenantSettingService.update(WorkerManagerSettings.SKIP_CONTENT_TYPES_NAME , Arrays.asList(CONTENT_TYPE_TO_SKIP));
        workerConfigRepo.deleteAll();
    }

    @Test
    public void handleInvalidRequestHeaders() throws InterruptedException {
        broadcastMessage(RawMessageBuilder.build(getDefaultTenant(), null, null,
                                                 null, null,
                                                 BODY_CONTENT.getBytes(StandardCharsets.UTF_8)),Optional.empty());
        Thread.sleep(1_000);
        Assert.assertEquals("There should be no requests created",0L, requestRepository.count());
        Assert.assertEquals("As the requestId is not provided the response should not be sent",0L,
                            responseMock.getEvents().size());

        SessionHelper.checkSession(stepPropertyUpdateRepository, DEFAULT_SOURCE, DEFAULT_SESSION, DEFAULT_WORKER, 0,0,0,
                     0, 0, 0,0,0,0);
    }

    @Test
    public void handleMissingRequestHeaders() {
        Message event = RawMessageBuilder.build(getDefaultTenant(), null, null,
                                           null, UUID.randomUUID().toString(),
                                           BODY_CONTENT.getBytes(StandardCharsets.UTF_8));
        broadcastMessage(event, Optional.empty());
        waitForResponses(1, 5, TimeUnit.SECONDS);
        Assert.assertEquals("There should be no requests created",0L, requestRepository.count());
        Assert.assertEquals("As the requestId and tenant are provided the response should be sent",1L, responseMock.getEvents().size());
        Assert.assertEquals("Invalid response status", ResponseStatus.SKIPPED,
                            responseMock.getEvents().stream().findFirst().get().getStatus());

        SessionHelper.checkSession(stepPropertyUpdateRepository, DEFAULT_SOURCE, DEFAULT_SESSION, DEFAULT_WORKER, 0,0,0,
                     0, 0, 0,0,0,0);
    }

    @Test
    public void handleSkipRequestByContentType() throws InterruptedException {
        broadcastMessage(createEvent(Optional.of(CONTENT_TYPE_TO_SKIP)), Optional.empty());
        Thread.sleep(1_000);
        Assert.assertEquals("There should be no requests created",0L, requestRepository.count());

        SessionHelper.checkSession(stepPropertyUpdateRepository, DEFAULT_SOURCE, DEFAULT_SESSION, DEFAULT_WORKER, 0,0,0,
                     0, 0, 0,0,0,0);
    }

    @Test
    public void dispatchAvailableRequests() throws Throwable {
        List<Request> requests = new ArrayList<>();
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.ERROR));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.ERROR));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.NO_WORKER_AVAILABLE));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.NO_WORKER_AVAILABLE));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.RUNNING));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.DISPATCHED));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.INVALID_CONTENT));
        requestRepository.saveAll(requests);

        // Simulate new conf for worker. So request in status NO_WORKER_AVAILABLE can be sent
        Assert.assertTrue("Error during worker conf import", workerConfigService.importConfiguration(Sets.newHashSet(
                new WorkerConfigDto(RequestHandlerConfiguration.AVAILABLE_WORKER_TYPE,
                                    Sets.newHashSet(RequestHandlerConfiguration.AVAILABLE_CONTENT_TYPE)))).isEmpty());

        // Scan
        requestScanService.scanNoWorkerAvailableRequests();

        Assert.assertTrue(waitForRequests(0, RequestStatus.NO_WORKER_AVAILABLE, 30, TimeUnit.SECONDS));
        Assert.assertTrue(waitForRequests(1, RequestStatus.RUNNING, 30, TimeUnit.SECONDS));
        Assert.assertTrue(waitForRequests(1, RequestStatus.INVALID_CONTENT, 30, TimeUnit.SECONDS));
        Assert.assertTrue(waitForRequests(2, RequestStatus.ERROR, 30, TimeUnit.SECONDS));
        Assert.assertTrue(waitForRequests(3, RequestStatus.DISPATCHED, 30, TimeUnit.SECONDS));

        // Wait for all session properties update received :
        // -2 NO_WORKER_AVAILABLE
        // +2 TO_DISPATCH
        // -2 TO_DISPATCH
        // +2 DISPATCHED
        waitForSessionProperties(4, 5, TimeUnit.SECONDS);
        SessionHelper.checkSession(stepPropertyUpdateRepository, DEFAULT_SOURCE, DEFAULT_SESSION, DEFAULT_WORKER, 0,0,-2,
                                   2, 0, 0,0,0,0);
    }

    @Test
    public void retryRequests() {

        List<Request> requests = new ArrayList<>();
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.ERROR));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.ERROR));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.NO_WORKER_AVAILABLE));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.NO_WORKER_AVAILABLE));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.RUNNING));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.DISPATCHED));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.INVALID_CONTENT));
        requestRepository.saveAll(requests);
        requestService.scheduleRequestRetryJob(new SearchRequestParameters());

        waitForRequests(4, RequestStatus.TO_DISPATCH, 10, TimeUnit.SECONDS);
        waitForRequests(4, RequestStatus.DISPATCHED, 10, TimeUnit.SECONDS);

        // Wait for all session properties update received :
        // -2 NO_WORKER_AVAILABLE
        // -2 ERROR
        // -1 INVALID_CONTENT
        // +5 TO_DISPATCH
        // -5 TO_DISPATCH
        // +5 DISPATCHED
        waitForSessionProperties(5, 5, TimeUnit.SECONDS);
        SessionHelper.checkSession(stepPropertyUpdateRepository, DEFAULT_SOURCE, DEFAULT_SESSION, DEFAULT_WORKER, 0,0,-2,
                                   5, 0, -2,-1,0,0);

    }

    @Test
    public void deleteRequests() {
        List<Request> requests = new ArrayList<>();
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.ERROR));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.ERROR));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.NO_WORKER_AVAILABLE));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.NO_WORKER_AVAILABLE));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.RUNNING));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.DISPATCHED));
        requests.add(createRequest(UUID.randomUUID().toString(), RequestStatus.INVALID_CONTENT));
        requestRepository.saveAll(requests);
        requestService.scheduleRequestDeletionJob(new SearchRequestParameters());

        waitForRequests(4, RequestStatus.TO_DELETE, 10, TimeUnit.SECONDS);
        waitForRequests(3, 10, TimeUnit.SECONDS);

        // Wait for all session properties update received :
        // -2 NO_WORKER_AVAILABLE
        // -2 ERROR
        // -1 INVALID_CONTENT
        // +5 TO_DELETE
        // -5 TO_DELETE
        // -5 TOTAL
        waitForSessionProperties(5, 5, TimeUnit.SECONDS);
        SessionHelper.checkSession(stepPropertyUpdateRepository, DEFAULT_SOURCE, DEFAULT_SESSION, DEFAULT_WORKER, -5,0,-2,
                                   0, 0, -2,-1,0,0);
    }

    @Test
    public void handleRequestAlreadyExists() {
        Message message = createEvent(Optional.of(RequestHandlerConfiguration.AVAILABLE_CONTENT_TYPE));
        String requestId = message.getMessageProperties().getHeader(EventHeadersHelper.REQUEST_ID_HEADER );
        Request request = new Request();
        request.setStatus(RequestStatus.NO_WORKER_AVAILABLE);
        request.setRequestId(requestId);
        request.setContent("toto".getBytes(StandardCharsets.UTF_8));
        request.setCreationDate(OffsetDateTime.now());
        request.setContentType(DEFAULT_CONTENT_TYPE);
        request.setSession("session");
        request.setSource("source");
        requestRepository.save(request);

        broadcastMessage(message, Optional.empty());
        Assert.assertTrue("Invalid number of responses", waitForResponses(1, 5, TimeUnit.SECONDS));
        Assert.assertEquals("Invalid number of responses", 1L, responseMock.getEvents().size());
        Assert.assertEquals("Invalid response status",ResponseStatus.SKIPPED, responseMock.getEvents().stream().findFirst().get().getStatus());

        SessionHelper.checkSession(stepPropertyUpdateRepository, DEFAULT_SOURCE, DEFAULT_SESSION, DEFAULT_WORKER, 0,0,0,
                     0, 0, 0,0,0,0);
        SessionHelper.checkSession(stepPropertyUpdateRepository, "source", "session", DEFAULT_WORKER, 0,0,0,
                                   0, 0, 0,0,0,0);
    }

    @Test
    public void handleValidRequest() {
        Message message = createEvent(Optional.of(RequestHandlerConfiguration.AVAILABLE_CONTENT_TYPE));
        String requestId = message.getMessageProperties().getHeader(EventHeadersHelper.REQUEST_ID_HEADER );
        broadcastMessage(message, Optional.empty());

        waitForResponses(1, 5, TimeUnit.SECONDS);
        waitForWorkerRequestResponses(1, 2, TimeUnit.SECONDS);

        Assert.assertEquals("Number of sent requests to worker invalid",1L,workerRequestMock.getEvents().size());
        Assert.assertEquals("Worker request content is invalid",BODY_CONTENT,
                            new String(workerRequestMock.getRawEvents().stream().findFirst().get().getBody()));
        Assert.assertEquals("There should one request created",1L, requestRepository.count());
        Optional<Request> request = requestRepository.findOneByRequestId(requestId);
        Assert.assertTrue("Request should be created in db",request.isPresent());
        Assert.assertEquals("Request status should be DISPATCHED", RequestStatus.DISPATCHED, request.get().getStatus());
        Assert.assertEquals("Invalid number of response event sent",1L, responseMock.getEvents().size());
        Assert.assertEquals("Invalid response status", ResponseStatus.GRANTED,
                            responseMock.getEvents().stream().findFirst().get().getStatus());

        SessionHelper.checkSession(stepPropertyUpdateRepository, DEFAULT_SOURCE, DEFAULT_SESSION, DEFAULT_WORKER, 1,0,0,
                     1, 0, 0,0,0,0);
    }

    @Test
    public void handleValidAndInvalidRequest() {
        List<Message> events = new ArrayList<>();
        events.add(createEvent(Optional.of(RequestHandlerConfiguration.AVAILABLE_CONTENT_TYPE)));
        events.add(createEvent(Optional.empty()));
        events.add(createEvent(Optional.of(RequestHandlerConfiguration.AVAILABLE_CONTENT_TYPE)));
        events.add(createEvent(Optional.empty()));
        events.add(createEvent(Optional.of(RequestHandlerConfiguration.AVAILABLE_CONTENT_TYPE)));
        events.add(createEvent(Optional.empty()));
        events.add(createEvent(Optional.of(RequestHandlerConfiguration.AVAILABLE_CONTENT_TYPE)));
        events.add(createEvent(Optional.of("unknown")));
        broadcastMessages(events, Optional.empty());
        waitForResponses(8, 5, TimeUnit.SECONDS);
        waitForWorkerRequestResponses(4, 2, TimeUnit.SECONDS);

        Assert.assertEquals("Number of sent requests to worker invalid",4L,workerRequestMock.getEvents().size());
        Assert.assertEquals("There should be 5 requests created",5L, requestRepository.count());
        Assert.assertEquals("There should be 4 requests created in dispatched state", 4L, requestRepository.findByStatus(
                RequestStatus.DISPATCHED).size());
        Assert.assertEquals("There should be 1 requests created in no worker available state", 1L, requestRepository.findByStatus(
                RequestStatus.NO_WORKER_AVAILABLE).size());
        Assert.assertEquals("Invalid number of response event sent",8L, responseMock.getEvents().size());
        Assert.assertEquals("Invalid response event status", 4L,
                responseMock.getEvents().stream().filter(e -> e.getStatus() == ResponseStatus.GRANTED).count());
        Assert.assertEquals("Invalid response event status", 3L,
                responseMock.getEvents().stream().filter(e -> e.getStatus() == ResponseStatus.SKIPPED).count());
        Assert.assertEquals("Invalid response event status", 1L,
                            responseMock.getEvents().stream().filter(e -> e.getStatus() == ResponseStatus.DELAYED).count());

        SessionHelper.checkSession(stepPropertyUpdateRepository, DEFAULT_SOURCE, DEFAULT_SESSION, DEFAULT_WORKER,
                                   5,0,1, 4, 0, 0,0,0,0);
    }

    @Test
    public void perfTest() {
        List<Message> events = new ArrayList<>();
        LOGGER.info("Start to send events");
        for (int i=0; i< 1_000;i++) {
            events.add(createEvent(Optional.of(RequestHandlerConfiguration.AVAILABLE_CONTENT_TYPE)));
            if (events.size() > 500) {
                broadcastMessages(events, Optional.empty());
                events.clear();
            }
        }
        broadcastMessages(events, Optional.empty());

        waitForResponses(1_000, 6, TimeUnit.SECONDS);
        waitForWorkerRequestResponses(1_000, 4, TimeUnit.SECONDS);

        Assert.assertEquals("invalid number of response event sent", 1_000, responseMock.getEvents().size());
        Assert.assertEquals("invalid number of worker request sent", 1_000, workerRequestMock.getEvents().size());

        Assert.assertEquals("There should be 5_000 requests created",1_000, requestRepository.count());
        Assert.assertEquals("There should be 5_000 requests created",1_000, requestRepository.findByStatus(RequestStatus.DISPATCHED).size());
        Assert.assertEquals("Invalid number of response event sent",1_000, responseMock.getEvents().size());
        Assert.assertEquals("Invalid response event status", 1_000,
                responseMock.getEvents().stream().filter(e -> e.getStatus() == ResponseStatus.GRANTED).count());

        SessionHelper.checkSession(stepPropertyUpdateRepository, DEFAULT_SOURCE, DEFAULT_SESSION, DEFAULT_WORKER,
                                   1_000,0,0, 1_000, 0, 0,0,0,0);
    }

}

