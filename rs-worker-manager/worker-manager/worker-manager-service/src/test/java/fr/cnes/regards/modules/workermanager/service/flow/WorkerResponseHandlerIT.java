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

import com.google.common.collect.Lists;
import fr.cnes.regards.modules.workercommon.dto.WorkerResponseStatus;
import fr.cnes.regards.modules.workermanager.amqp.events.in.WorkerResponseEvent;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseStatus;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestDTO;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.sessions.SessionHelper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@ActiveProfiles(value = { "default", "test", "testAmqp" }, inheritProfiles = false)
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=worker_responses", "regards.amqp.enabled=true",
        "regards.workermanager.worker.response.bulk.size=1000" },
    locations = { "classpath:application-test.properties" })
public class WorkerResponseHandlerIT extends AbstractWorkerManagerIT {

    /**
     * Check that requests are updated in database with RUNNING status after RUNNING response sent by worker
     *
     * @throws InterruptedException
     */
    @Test
    public void handleGrantedResponseFromWorker() throws InterruptedException {
        // Create request in database
        Request request = requestRepository.save(createRequest("1", RequestStatus.DISPATCHED));

        // Simulate new event from worker
        publishWorkerResponse(createWorkerResponseEvent("1", WorkerResponseStatus.RUNNING));
        Thread.sleep(2_000);

        Optional<RequestDTO> dto = requestService.get(request.getRequestId());
        Assert.assertTrue("Request should exists", dto.isPresent());
        Assert.assertEquals("Requests status invalid", RequestStatus.RUNNING, dto.get().getStatus());
        Assert.assertEquals("Invalid number of response event sent", 0L, responseMock.getEvents().size());

        SessionHelper.checkSession(stepPropertyUpdateRepository,
                                   DEFAULT_SOURCE,
                                   DEFAULT_SESSION,
                                   DEFAULT_WORKER,
                                   0,
                                   1,
                                   0,
                                   -1,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0);
    }

    /**
     * Check that requests are updated in database with SUCCESS status after SUCCESS response sent by worker
     */
    @Test
    public void handleSuccessResponseFromWorker() {
        // Create request in database
        Request request = requestRepository.save(createRequest("1", RequestStatus.RUNNING));

        // Simulate new event from worker
        publishWorkerResponse(createWorkerResponseEvent("1", WorkerResponseStatus.SUCCESS));
        waitForResponses(1, 5, TimeUnit.SECONDS);

        Optional<RequestDTO> dto = requestService.get(request.getRequestId());
        Assert.assertFalse("Request should noy exists anymore", dto.isPresent());
        Assert.assertEquals("Invalid number of response event sent", 1L, responseMock.getEvents().size());
        Assert.assertEquals("Invalid response event status",
                            ResponseStatus.SUCCESS,
                            responseMock.getEvents().stream().findFirst().get().getState());

        SessionHelper.checkSession(stepPropertyUpdateRepository,
                                   DEFAULT_SOURCE,
                                   DEFAULT_SESSION,
                                   DEFAULT_WORKER,
                                   0,
                                   -1,
                                   0,
                                   0,
                                   1,
                                   0,
                                   0,
                                   0,
                                   0);
    }

    /**
     * Check that requests are updated in database with INVALID_CONTENT status after INVALID_CONTENT response sent by worker
     */
    @Test
    public void handleInvalidContentResponseFromWorker() {
        // Create request in database
        Request request = requestRepository.save(createRequest("1", RequestStatus.RUNNING));
        // Simulate new event from worker
        publishWorkerResponse(createWorkerResponseEvent("1", WorkerResponseStatus.INVALID_CONTENT));

        waitForResponses(1, 5, TimeUnit.SECONDS);

        Optional<RequestDTO> dto = requestService.get(request.getRequestId());
        Assert.assertTrue("Request should exists", dto.isPresent());
        Assert.assertEquals("Requests status invalid", RequestStatus.INVALID_CONTENT, dto.get().getStatus());
        Assert.assertEquals("Invalid number of response event sent", 1L, responseMock.getEvents().size());
        Assert.assertEquals("Invalid response event status",
                            ResponseStatus.INVALID_CONTENT,
                            responseMock.getEvents().stream().findFirst().get().getState());

        SessionHelper.checkSession(stepPropertyUpdateRepository,
                                   DEFAULT_SOURCE,
                                   DEFAULT_SESSION,
                                   DEFAULT_WORKER,
                                   0,
                                   -1,
                                   0,
                                   0,
                                   0,
                                   0,
                                   1,
                                   0,
                                   0);
    }

    /**
     * Check that requests are updated in database with ERROR status after ERROR response sent by worker
     */
    @Test
    public void handleErrorResponseFromWorker() {
        // Create request in database
        Request request = requestRepository.save(createRequest("1", RequestStatus.RUNNING));

        // Simulate new event from worker
        publishWorkerResponse(createWorkerResponseEvent("1", WorkerResponseStatus.ERROR));
        waitForResponses(1, 5, TimeUnit.SECONDS);

        Optional<RequestDTO> dto = requestService.get(request.getRequestId());
        Assert.assertTrue("Request should exists", dto.isPresent());
        Assert.assertEquals("Requests status invalid", RequestStatus.ERROR, dto.get().getStatus());
        Assert.assertEquals("Invalid number of response event sent", 1L, responseMock.getEvents().size());
        Assert.assertEquals("Invalid response event status",
                            ResponseStatus.ERROR,
                            responseMock.getEvents().stream().findFirst().get().getState());

        SessionHelper.checkSession(stepPropertyUpdateRepository,
                                   DEFAULT_SOURCE,
                                   DEFAULT_SESSION,
                                   DEFAULT_WORKER,
                                   0,
                                   -1,
                                   0,
                                   0,
                                   0,
                                   1,
                                   0,
                                   0,
                                   0);
    }

    /**
     * Check that requests are updated in database with ERROR status after worker sent to DLQ the request
     */
    @Test
    public void handleDlqErrorFromWorker() {

        handleDlqErrorFromWorker(RequestStatus.RUNNING, RequestStatus.ERROR);

        SessionHelper.checkSession(stepPropertyUpdateRepository,
                                   DEFAULT_SOURCE,
                                   DEFAULT_SESSION,
                                   DEFAULT_WORKER,
                                   0,
                                   -1,
                                   0,
                                   0,
                                   0,
                                   1,
                                   0,
                                   0,
                                   0);
    }

    /**
     * Check that requests are not updated in database with ERROR status after worker sent to DLQ the request for request
     * scheduled to be dispatched. Retry case of a request.
     */
    @Test
    public void handleDlqErrorDuringReDispatchFromWorker() {
        handleDlqErrorFromWorker(RequestStatus.TO_DISPATCH, RequestStatus.TO_DISPATCH);

        SessionHelper.checkSession(stepPropertyUpdateRepository,
                                   DEFAULT_SOURCE,
                                   DEFAULT_SESSION,
                                   DEFAULT_WORKER,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0);
    }

    /**
     * Check that requestId from header is used to identify request to update after a response is sent from worker.
     */
    @Test
    public void handleInvalidResponseFromWorker() {
        // Create request in database
        Request request = requestRepository.save(createRequest("1", RequestStatus.DISPATCHED));

        // Simulate new event from worker
        publishWorkerResponse(createWorkerResponseEvent("2", WorkerResponseStatus.RUNNING));
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        Optional<RequestDTO> dto = requestService.get(request.getRequestId());
        Assert.assertTrue("Request should exists", dto.isPresent());
        Assert.assertEquals("Requests status invalid", RequestStatus.DISPATCHED, dto.get().getStatus());

        SessionHelper.checkSession(stepPropertyUpdateRepository,
                                   DEFAULT_SOURCE,
                                   DEFAULT_SESSION,
                                   DEFAULT_WORKER,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0);
    }

    @Test
    public void perfTest() {
        List<Request> requests = Lists.newArrayList();
        List<WorkerResponseEvent> responses = Lists.newArrayList();
        for (int i = 0; i < 1_000; i++) {
            String requestId = "request_" + String.valueOf(i);
            requests.add(createRequest("request_" + String.valueOf(i), RequestStatus.RUNNING));
            responses.add(createWorkerResponseEvent(requestId, WorkerResponseStatus.SUCCESS));
        }
        requestRepository.saveAll(requests);
        publishWorkerResponses(responses);
        waitForResponses(1_000, 3, TimeUnit.SECONDS);

        Assert.assertEquals("Invalid number of response event sent", 1_000L, responseMock.getEvents().size());
        Assert.assertFalse("Invalid response event status",
                           responseMock.getEvents().stream().anyMatch(e -> e.getState() != ResponseStatus.SUCCESS));

        SessionHelper.checkSession(stepPropertyUpdateRepository,
                                   DEFAULT_SOURCE,
                                   DEFAULT_SESSION,
                                   DEFAULT_WORKER,
                                   0,
                                   -1_000,
                                   0,
                                   0,
                                   1_000,
                                   0,
                                   0,
                                   0,
                                   0);
    }

    private void handleDlqErrorFromWorker(RequestStatus initialStatus, RequestStatus finalStatus) {
        // Create request in database
        Request request = requestRepository.save(createRequest("1", initialStatus));

        // Simulate request sent to DLQ by worker
        String errorStackTrace = "Test error stacktrace";
        publishWorkerDlq(createWorkerDlqRequestEvent("1", "Test error stacktrace"));
        waitForResponses(1, 5, TimeUnit.SECONDS);

        Optional<RequestDTO> dto = requestService.get(request.getRequestId());
        Assert.assertTrue("Request should exists", dto.isPresent());
        Assert.assertEquals("Request status invalid", finalStatus, dto.get().getStatus());
        if (finalStatus == RequestStatus.ERROR) {
            Assert.assertEquals("Request error should not be empty", errorStackTrace, dto.get().getError());
        } else {
            Assert.assertNull("Request error should not be present", dto.get().getError());
        }
        Assert.assertEquals("Invalid number of response event sent", 0L, responseMock.getEvents().size());
    }
}
