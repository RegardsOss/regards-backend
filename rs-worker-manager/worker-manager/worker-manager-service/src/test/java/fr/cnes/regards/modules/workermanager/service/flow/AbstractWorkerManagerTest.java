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

import com.google.common.collect.Maps;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.modules.workercommon.dto.WorkerResponseStatus;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.events.EventHeaders;
import fr.cnes.regards.modules.workermanager.dto.events.RawMessageBuilder;
import fr.cnes.regards.modules.workermanager.dto.events.in.RequestEvent;
import fr.cnes.regards.modules.workermanager.dto.events.in.WorkerResponseEvent;
import fr.cnes.regards.modules.workermanager.dto.events.out.WorkerRequestEvent;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.flow.mock.ResponseMockHandler;
import fr.cnes.regards.modules.workermanager.service.flow.mock.WorkerRequestMockHandler;
import fr.cnes.regards.modules.workermanager.service.requests.RequestService;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.bouncycastle.cert.ocsp.Req;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class AbstractWorkerManagerTest extends AbstractRegardsServiceIT  {

    public static final String BODY_CONTENT = "{\"test\":\"value\",\"map\": { \"values\": [1,2,3]}}";

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

    protected Request createRequest(String requestId,RequestStatus status) {
        Request request = new Request();
        request.setRequestId(requestId);
        request.setStatus(status);
        request.setContent(BODY_CONTENT.getBytes(StandardCharsets.UTF_8));
        request.setContentType("content1");
        request.setSession("Session1");
        request.setSource("Source");
        request.setCreationDate(OffsetDateTime.now());
        request.setDispatchedWorkerType("Worker1");
        return request;
    }

    protected WorkerResponseEvent createWorkerResponseEvent(String requestId, WorkerResponseStatus status) {
        WorkerResponseEvent event = new WorkerResponseEvent();
        event.setHeader(EventHeaders.REQUEST_ID_HEADER.getName(), requestId);
        event.setStatus(status);
        return event;
    }

    protected WorkerRequestEvent createWorkerDlqRequestEvent(String requestId, String stackStrace) {
        WorkerRequestEvent event = new WorkerRequestEvent();
        event.setHeader(EventHeaders.REQUEST_ID_HEADER.getName(), requestId);
        event.setHeader(EventHeaders.DLQ_ERROR_STACKTRACE_HEADER.getName(), stackStrace);
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
                                       "REGARDS", "it tests", UUID.randomUUID().toString(),
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
