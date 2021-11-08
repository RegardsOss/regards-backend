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

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.workermanager.dao.IRequestRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerManagerSettings;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.dto.events.RawMessageBuilder;
import fr.cnes.regards.modules.workermanager.dto.events.out.ResponseStatus;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

@ActiveProfiles(value = { "default", "test", "testAmqp" }, inheritProfiles = false)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=worker_manager_it",
        "regards.amqp.enabled=true","regards.workermanager.request.bulk.size=1000" },
        locations = { "classpath:application-test.properties" })
@ContextConfiguration(classes = { RequestHandlerConfiguration.class } )
public class RequestHandlerTest extends AbstractWorkerManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandlerTest.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IRequestRepository repository;

    @Autowired
    private DynamicTenantSettingService tenantSettingService;

    private final static String CONTENT_TYPE_TO_SKIP = "toskip";

    @Before
    public void init() throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        repository.deleteAll();
        responseMock.reset();
        workerRequestMock.reset();
        tenantSettingService.update(WorkerManagerSettings.SKIP_CONTENT_TYPES_NAME , Arrays.asList(CONTENT_TYPE_TO_SKIP));
    }

    @Test
    public void handleInvalidRequestHeaders() throws InterruptedException {
        broadcastMessage(RawMessageBuilder.build(getDefaultTenant(), null, null,
                                                 null, null,
                                                 BODY_CONTENT.getBytes(StandardCharsets.UTF_8)),Optional.empty());
        Thread.sleep(1_000);
        Assert.assertEquals("There should be no requests created",0L, repository.count());
        Assert.assertEquals("As the requestId is not provided the response should not be sent",0L, responseMock.getEvents().size());
    }

    @Test
    public void handleMissingRequestHeaders() {
        Message event = RawMessageBuilder.build(getDefaultTenant(), null, null,
                                           null, UUID.randomUUID().toString(),
                                           BODY_CONTENT.getBytes(StandardCharsets.UTF_8));
        broadcastMessage(event, Optional.empty());
        waitForResponses(1, 5, TimeUnit.SECONDS);
        Assert.assertEquals("There should be no requests created",0L, repository.count());
        Assert.assertEquals("As the requestId and tenant are provided the response should be sent",1L, responseMock.getEvents().size());
        Assert.assertEquals("Invalid response status", ResponseStatus.SKIPPED,
                            responseMock.getEvents().stream().findFirst().get().getStatus());
    }

    @Test
    public void handleSkipRequestByContentType() throws InterruptedException {
        broadcastMessage(createEvent(Optional.of(CONTENT_TYPE_TO_SKIP)), Optional.empty());
        Thread.sleep(1_000);
        Assert.assertEquals("There should be no requests created",0L, repository.count());
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
        Assert.assertEquals("There should one request created",1L, repository.count());
        Optional<Request> request = repository.findOneByRequestId(requestId);
        Assert.assertTrue("Request should be created in db",request.isPresent());
        Assert.assertEquals("Request status should be DISPATCHED", RequestStatus.DISPATCHED, request.get().getStatus());
        Assert.assertEquals("Invalid number of response event sent",1L, responseMock.getEvents().size());
        Assert.assertEquals("Invalid response status", ResponseStatus.GRANTED,
                            responseMock.getEvents().stream().findFirst().get().getStatus());
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
        Assert.assertEquals("There should be 5 requests created",5L, repository.count());
        Assert.assertEquals("There should be 4 requests created in dispatched state", 4L, repository.findByStatus(
                RequestStatus.DISPATCHED).size());
        Assert.assertEquals("There should be 1 requests created in no worker available state", 1L, repository.findByStatus(
                RequestStatus.NO_WORKER_AVAILABLE).size());
        Assert.assertEquals("Invalid number of response event sent",8L, responseMock.getEvents().size());
        Assert.assertEquals("Invalid response event status", 4L,
                responseMock.getEvents().stream().filter(e -> e.getStatus() == ResponseStatus.GRANTED).count());
        Assert.assertEquals("Invalid response event status", 3L,
                responseMock.getEvents().stream().filter(e -> e.getStatus() == ResponseStatus.SKIPPED).count());
        Assert.assertEquals("Invalid response event status", 1L,
                            responseMock.getEvents().stream().filter(e -> e.getStatus() == ResponseStatus.DELAYED).count());
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

        Assert.assertEquals("There should be 5_000 requests created",1_000, repository.count());
        Assert.assertEquals("There should be 5_000 requests created",1_000, repository.findByStatus(RequestStatus.DISPATCHED).size());
        Assert.assertEquals("Invalid number of response event sent",1_000, responseMock.getEvents().size());
        Assert.assertEquals("Invalid response event status", 1_000,
                responseMock.getEvents().stream().filter(e -> e.getStatus() == ResponseStatus.GRANTED).count());
    }

}

