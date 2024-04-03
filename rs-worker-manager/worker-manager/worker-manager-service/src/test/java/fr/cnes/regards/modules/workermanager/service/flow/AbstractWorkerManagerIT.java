/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.amqp.batch.RetryBatchMessageHandler;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.event.IEvent;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.modules.workercommon.dto.WorkerResponseStatus;
import fr.cnes.regards.modules.workermanager.amqp.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.amqp.events.RawMessageBuilder;
import fr.cnes.regards.modules.workermanager.amqp.events.in.RequestEvent;
import fr.cnes.regards.modules.workermanager.amqp.events.in.WorkerRequestDlqEvent;
import fr.cnes.regards.modules.workermanager.amqp.events.in.WorkerResponseEvent;
import fr.cnes.regards.modules.workermanager.amqp.events.out.WorkerRequestEvent;
import fr.cnes.regards.modules.workermanager.dao.IRequestRepository;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.flow.mock.ResponseMockHandler;
import fr.cnes.regards.modules.workermanager.service.flow.mock.WorkerRequestDlqMockHandler;
import fr.cnes.regards.modules.workermanager.service.flow.mock.WorkerRequestMockHandler;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import fr.cnes.regards.modules.workermanager.service.sessions.SessionHelper;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class AbstractWorkerManagerIT extends AbstractRegardsServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWorkerManagerIT.class);

    public static final String BODY_CONTENT = "{\"test\":\"value\",\"map\": { \"values\": [1,2,3]}}";

    public final static String DEFAULT_SOURCE = "REGARDS";

    public final static String DEFAULT_SESSION = "test-it";

    public final static String DEFAULT_WORKER = "WorkerTest";

    public final static String DEFAULT_CONTENT_TYPE = RequestHandlerConfiguration.AVAILABLE_CONTENT_TYPE_0;

    @SpyBean
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
    protected WorkerRequestDlqMockHandler workerRequestDlqMockHandler;

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

    protected SessionHelper sessionHelper;

    @Before
    public void initTests() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        requestRepository.deleteAllInBatch();
        jobInfoRepo.deleteAll();
        stepPropertyUpdateRepository.deleteAllInBatch();
        sessionSnapshotRepository.deleteAllInBatch();
        sessionStepRepository.deleteAllInBatch();
        responseMock.reset();
        workerRequestMock.reset();
        sessionHelper = new SessionHelper(runtimeTenantResolver, getDefaultTenant(), stepPropertyUpdateRepository);
    }

    protected Request createRequest(String requestId,
                                    RequestStatus status,
                                    String contentType,
                                    int initStepNumber,
                                    String initStepWorkerType) {
        Request request = new Request();
        request.setRequestId(requestId);
        request.setStatus(status);
        request.setContent(BODY_CONTENT.getBytes(StandardCharsets.UTF_8));
        request.setOriginalContent(request.getContent());
        request.setContentType(contentType);
        request.setSession(DEFAULT_SESSION);
        request.setSource(DEFAULT_SOURCE);
        request.setCreationDate(OffsetDateTime.now());
        request.setStepNumber(initStepNumber);
        request.setStepWorkerType(initStepWorkerType);
        if (status != RequestStatus.NO_WORKER_AVAILABLE) {
            request.setDispatchedWorkerType(DEFAULT_WORKER);
        }
        return request;
    }

    protected Request createRequest(String requestId, RequestStatus status) {
        return createRequest(requestId,
                             status,
                             DEFAULT_CONTENT_TYPE,
                             0,
                             RequestHandlerConfiguration.AVAILABLE_WORKER_TYPE_1);
    }

    protected WorkerResponseEvent createWorkerResponseEvent(String requestId, WorkerResponseStatus status) {
        WorkerResponseEvent event = new WorkerResponseEvent();
        event.setHeader(EventHeadersHelper.REQUEST_ID_HEADER, requestId);
        event.setStatus(status);
        return event;
    }

    protected WorkerRequestDlqEvent createWorkerDlqRequestEvent(String requestId, String stackStrace) {
        WorkerRequestDlqEvent event = new WorkerRequestDlqEvent();
        event.setHeader(EventHeadersHelper.REQUEST_ID_HEADER, requestId);
        event.setHeader(EventHeadersHelper.DLQ_ERROR_STACKTRACE_HEADER, stackStrace);
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

    protected IEvent createEvent(Optional<String> contentType) {
        return RawMessageBuilder.build(getDefaultTenant(),
                                       contentType.orElse(null),
                                       DEFAULT_SOURCE,
                                       DEFAULT_SESSION,
                                       UUID.randomUUID().toString(),
                                       null,
                                       BODY_CONTENT.getBytes(StandardCharsets.UTF_8));
    }

    protected boolean waitForResponses(int expected, long timeout, TimeUnit timeUnit) {
        try {
            Awaitility.await().atMost(timeout, timeUnit).until(() -> responseMock.getEvents().size() >= expected);

            return responseMock.getEvents().size() == expected;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }

    protected boolean waitForWorkerRequestResponses(int expected, long timeout, TimeUnit timeUnit) {
        try {
            Awaitility.await().atMost(timeout, timeUnit).until(() -> workerRequestMock.getEvents().size() >= expected);
            return workerRequestMock.getEvents().size() == expected;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }

    protected boolean waitForWorkerRequestDlqResponses(int expected, long timeout, TimeUnit timeUnit) {
        try {
            Awaitility.await()
                      .atMost(timeout, timeUnit)
                      .until(() -> workerRequestDlqMockHandler.getEvents().size() >= expected);
            return workerRequestDlqMockHandler.getEvents().size() == expected;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }

    protected boolean waitForRequests(int expected, RequestStatus status, long count, TimeUnit timeUnit) {
        String tenant = getDefaultTenant();
        try {
            Awaitility.await().atMost(count, timeUnit).until(() -> {
                runtimeTenantResolver.forceTenant(tenant);
                return requestRepository.findByStatus(status).size() == expected;
            });
            return requestRepository.findByStatus(status).size() == expected;
        } catch (ConditionTimeoutException e) {
            LOGGER.error("ERROR waiting for {} requests in status {}. Got {} after {}{} ",
                         expected,
                         status.toString(),
                         requestRepository.findByStatus(status).size(),
                         count,
                         timeUnit.toString());
            return false;
        }
    }

    protected boolean waitForRequests(int expected, long timeout, TimeUnit timeUnit) {
        try {
            Awaitility.await().atMost(timeout, timeUnit).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return requestRepository.count() == expected;
            });
            return requestRepository.count() == expected;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }

    protected boolean waitForSessionProperties(int expected, long timeout, TimeUnit timeUnit) {
        try {
            Awaitility.await().atMost(timeout, timeUnit).until(() -> {
                runtimeTenantResolver.forceTenant(getDefaultTenant());
                return sessionStepRepository.count() == expected;
            });
            return sessionStepRepository.count() == expected;
        } catch (ConditionTimeoutException e) {
            return false;
        }
    }

    protected void broadcastMessage(IEvent message, Optional<String> routingKey) {
        publisher.broadcast(amqpAdmin.getBroadcastExchangeName(RequestEvent.class.getTypeName(),
                                                               Target.ONE_PER_MICROSERVICE_TYPE),
                            Optional.empty(),
                            routingKey,
                            Optional.empty(),
                            0,
                            message,
                            new HashMap<>());
    }

    protected void broadcastMessages(List<IEvent> messages, Optional<String> routingKey) {
        publisher.broadcastAll(amqpAdmin.getBroadcastExchangeName(RequestEvent.class.getTypeName(),
                                                                  Target.ONE_PER_MICROSERVICE_TYPE),
                               Optional.empty(),
                               routingKey,
                               Optional.empty(),
                               0,
                               messages,
                               new HashMap<>());
    }

    /**
     * Check if expected retry headers are present in AMQP messages retried after an unexpected failure.
     *
     * @param nbEvents          number of expected AMQP messages
     * @param nbExpectedRetries total number of retries
     * @param targetQueueName   name of the queue on which messages are published successfully after 'nbExpectedRetries retries
     */
    protected void verifyRetryHeaderAfterXFailures(int nbEvents,
                                                   int nbExpectedRetries,
                                                   String targetQueueName,
                                                   String retryExchangeName) {
        // messages has to re-published 'nbExpectedRetries' times
        ArgumentCaptor<Message> eventCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(publisher, Mockito.timeout(30_000L * nbExpectedRetries).times(nbEvents * nbExpectedRetries))
               .basicPublish(ArgumentMatchers.eq(getDefaultTenant()),
                             ArgumentMatchers.eq(retryExchangeName),
                             ArgumentMatchers.eq(targetQueueName),
                             eventCaptor.capture());
        // Retry header has to be updated
        org.assertj.core.api.Assertions.assertThat(eventCaptor.getAllValues()
                                                              .stream()
                                                              .filter(message -> (int) message.getMessageProperties()
                                                                                              .getHeader(
                                                                                                  RetryBatchMessageHandler.X_RETRY_HEADER)
                                                                                 == nbExpectedRetries))
                                       .as("Request retry header should be present and updated with the number of retries.")
                                       .hasSize(nbEvents);
    }
}
