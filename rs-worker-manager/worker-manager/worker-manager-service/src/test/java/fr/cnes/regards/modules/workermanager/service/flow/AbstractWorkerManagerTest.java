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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.modules.workercommon.dto.WorkerResponseStatus;
import fr.cnes.regards.modules.workermanager.dao.IRequestRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerManagerSettings;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.dto.events.RawMessageBuilder;
import fr.cnes.regards.modules.workermanager.dto.events.in.RequestEvent;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerRequestDlqEvent;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerResponseEvent;
import fr.cnes.regards.modules.workermanager.dto.events.out.WorkerRequestEvent;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.flow.mock.ResponseMockHandler;
import fr.cnes.regards.modules.workermanager.service.flow.mock.WorkerRequestMockHandler;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import fr.cnes.regards.modules.workermanager.service.sessions.SessionService;
import fr.cnes.regards.modules.workermanager.service.sessions.WorkerStepPropertyEnum;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class AbstractWorkerManagerTest extends AbstractRegardsServiceIT  {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorkerManagerTest.class);

    public static final String BODY_CONTENT = "{\"test\":\"value\",\"map\": { \"values\": [1,2,3]}}";

    public final static String DEFAULT_SOURCE = "REGARDS";
    public final static String DEFAULT_SESSION = "test-it";
    public final static String DEFAULT_WORKER = "WorkerTest";
    public final static String DEFAULT_CONTENT_TYPE = RequestHandlerConfiguration.AVAILABLE_CONTENT_TYPE;

    @Autowired
    protected IPublisher publisher;

    @Autowired
    protected IAmqpAdmin amqpAdmin;

    @Autowired
    protected RequestService requestService;

    @Autowired
    protected WorkerRequestMockHandler workerRequestMock;

    @Autowired
    protected ResponseMockHandler responseMock;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected IRequestRepository requestRepository;

    @Autowired
    protected IStepPropertyUpdateRequestRepository stepPropertyUpdateRepository;

    @Autowired
    protected ISessionStepRepository sessionStepRepository;

    @Autowired
    protected ISnapshotProcessRepository sessionSnapshotRepository;

    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    @Before
    public void initTests() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        requestRepository.deleteAll();
        jobInfoRepo.deleteAll();
        stepPropertyUpdateRepository.deleteAll();
        sessionSnapshotRepository.deleteAll();
        sessionStepRepository.deleteAll();
        responseMock.reset();
        workerRequestMock.reset();
    }

    protected Request createRequest(String requestId,RequestStatus status) {
        Request request = new Request();
        request.setRequestId(requestId);
        request.setStatus(status);
        request.setContent(BODY_CONTENT.getBytes(StandardCharsets.UTF_8));
        request.setContentType(DEFAULT_CONTENT_TYPE);
        request.setSession(DEFAULT_SESSION);
        request.setSource(DEFAULT_SOURCE);
        request.setCreationDate(OffsetDateTime.now());
        if (status != RequestStatus.NO_WORKER_AVAILABLE) {
            request.setDispatchedWorkerType(DEFAULT_WORKER);
        }
        return request;
    }

    protected WorkerResponseEvent createWorkerResponseEvent(String requestId, WorkerResponseStatus status) {
        WorkerResponseEvent event = new WorkerResponseEvent();
        event.setHeader(EventHeadersHelper.REQUEST_ID_HEADER , requestId);
        event.setStatus(status);
        return event;
    }

    protected WorkerRequestDlqEvent createWorkerDlqRequestEvent(String requestId, String stackStrace) {
        WorkerRequestDlqEvent event = new WorkerRequestDlqEvent();
        event.setHeader(EventHeadersHelper.REQUEST_ID_HEADER , requestId);
        event.setHeader(EventHeadersHelper.DLQ_ERROR_STACKTRACE_HEADER , stackStrace);
        return event;
    }

    protected void publishWorkerDlq(WorkerRequestEvent event) {
        publisher.publish(event, requestService.getWorkerRequestDlqName(), Optional.empty());
    }

    protected void publishWorkerResponse(WorkerResponseEvent event) {
        publisher.publish(event, requestService.getWorkerResponseQueueName(), Optional.empty());
    }

    protected void publishWorkerResponses(List<WorkerResponseEvent> events) {
        publisher.publish(events, requestService.getWorkerResponseQueueName(), Optional.empty());
    }

    protected Message createEvent(Optional<String> contentType) {
        return RawMessageBuilder.build(getDefaultTenant(), contentType.orElse(null),
                                       DEFAULT_SOURCE, DEFAULT_SESSION, UUID.randomUUID().toString(),
                                       BODY_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    protected boolean waitForResponses(int expected, long count, TimeUnit timeUnit) {
        try {
            Awaitility.await().atMost(count, timeUnit).until(() -> responseMock.getEvents().size() >= expected);

            return responseMock.getEvents().size() == expected;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }

    protected boolean waitForWorkerRequestResponses(int expected, long count, TimeUnit timeUnit) {
        try {
            Awaitility.await().atMost(count, timeUnit).until(() -> workerRequestMock.getEvents().size() >= expected);
            return workerRequestMock.getEvents().size() == expected;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }

    protected boolean waitForRequests(int expected, RequestStatus status, long count, TimeUnit timeUnit) {
        try {
            Awaitility.await().atMost(count, timeUnit).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return requestRepository.findByStatus(status).size() == expected;
            });
            return requestRepository.findByStatus(status).size() == expected;
        } catch (ConditionTimeoutException e) {
            LOGGER.error("ERROR waiting for {} requests in status {}. Git {} after {}{} ",
                         expected,status.toString(), requestRepository.findByStatus(status).size(), count, timeUnit.toString());
            return false;
        }
    }

    protected boolean waitForRequests(int expected, long count, TimeUnit timeUnit) {
        try {
            Awaitility.await().atMost(count, timeUnit).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return requestRepository.count() == expected;
            });
            return requestRepository.count() == expected;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }

    protected boolean waitForSessionProperties(int expected, long count, TimeUnit timeUnit) {
        try {
            Awaitility.await().atMost(count, timeUnit).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return sessionStepRepository.count() == expected;
            });
            return sessionStepRepository.count() == expected;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }


    protected void broadcastMessage(Message message,Optional<String> routingKey) {
        publisher.broadcast(amqpAdmin.getBroadcastExchangeName(RequestEvent.class.getTypeName(),
                                                               Target.ONE_PER_MICROSERVICE_TYPE),
                            Optional.empty(), routingKey, Optional.empty(), 0, message, new HashMap<>());
    }

    protected void broadcastMessages(List<Message> messages,Optional<String> routingKey) {
        publisher.broadcastAll(amqpAdmin.getBroadcastExchangeName(RequestEvent.class.getTypeName(),
                                                               Target.ONE_PER_MICROSERVICE_TYPE),
                            Optional.empty(), routingKey, Optional.empty(), 0, messages, new HashMap<>());
    }
}
