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
package fr.cnes.regards.modules.workermanager.service.requests;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.workercommon.dto.WorkerResponseStatus;
import fr.cnes.regards.modules.workermanager.amqp.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.amqp.events.in.WorkerHeartBeatEvent;
import fr.cnes.regards.modules.workermanager.amqp.events.in.WorkerResponseEvent;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerConfig;
import fr.cnes.regards.modules.workermanager.domain.config.Workflow;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.cache.AbstractWorkerManagerServiceUtilsIT;
import fr.cnes.regards.modules.workermanager.service.sessions.SessionsRequestsInfo;
import org.junit.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The purpose of this test is to verify if a workflow of workers is properly executed with the successive sending of
 * {@link Request}.
 *
 * <p>
 * TEST PLAN :
 * <ul>
 *  <li>Nominal cases :
 *    <ul>
 *      <li>{@link #workflow_nominal_worker_response_success()}</li>
 *    </ul></li>
 *  <li>Error cases :
 *    <ul>
 *      <li>{@link #workflow_error_worker_response_error()}</li>
 *      <li>{@link #workflow_error_worker_response_error_with_retry_success()}</li>
 *    </ul></li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=request_service_workflow_it" })
@ActiveProfiles(value = { "noscheduler", "nojobs" })
public class RequestServiceWorkflowIT extends AbstractWorkerManagerServiceUtilsIT {

    private static final String WORKFLOW_TYPE_1 = "workflowType1";

    private static final String WORKFLOW_TYPE_2 = "workflowType2";

    private static final String WORKER_TYPE_1 = "workerType1";

    private static final String WORKER_TYPE_2 = "workerType2";

    private static final String WORKER_TYPE_3 = "workerType3";

    private static final String CONTENT_TYPE_1 = "contentType1";

    private static final String CONTENT_TYPE_2 = "contentType2";

    private static final String CONTENT_TYPE_3 = "contentType3";

    private static final String CONTENT_BODY = "CONTENT BODY";

    private static final String CONTENT_BODY_RESPONSE = CONTENT_BODY + "-%d handled";

    private static final int INIT_STEP = 0;

    @Override
    protected void doInit() {
        initWorkers();
        initWorkflows();
    }

    /**
     * The following workflows are executed (each time in success)
     * WORKFLOW_TYPE_1 -> worker1, worker2
     * WORKFLOW_TYPE_2 -> worker3, worker1, worker2
     * <p>
     * With the call on handleWorkersResponses :
     * Check if the request is associated with a workflow
     * ° If yes, save the request in db with:
     * - IF remaining steps       : content = WorkerResponse.content AND step: step+1 AND status: TO_DISPATCH
     * - IF no more step or error : end of request, current behavior.
     */
    @Test
    @Purpose("Test successive executions of handleRequests in success on a workflow of workers")
    public void workflow_nominal_worker_response_success() {
        // -------------
        // --- GIVEN ---
        // -------------
        // create worker requests
        List<Request> requestsWorkflow1 = createRequests("requestIdWorkflowNominal1-",
                                                         OffsetDateTime.now(),
                                                         WORKFLOW_TYPE_1,
                                                         "sourceWorkflowNominal1",
                                                         "sessionWorkflowNominal1",
                                                         RequestStatus.TO_DISPATCH,
                                                         CONTENT_BODY.getBytes(),
                                                         null,
                                                         2);
        List<Request> requestsWorkflow2 = createRequests("requestIdWorkflowNominal2-",
                                                         OffsetDateTime.now(),
                                                         WORKFLOW_TYPE_2,
                                                         "sourceWorkflowNominal2",
                                                         "sessionWorkflowNominal2",
                                                         RequestStatus.TO_DISPATCH,
                                                         CONTENT_BODY.getBytes(),
                                                         null,
                                                         2);
        // -------------------
        // --- WHEN / THEN ---
        // -------------------
        // --> STEP 0
        // simulate and handle responses in success
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow1.size() + requestsWorkflow2.size(),
                                                            0,
                                                            CONTENT_BODY,
                                                            requestsWorkflow1.size() + requestsWorkflow2.size(),
                                                            RequestStatus.TO_DISPATCH,
                                                            1,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP)), false);

        // --> STEP 1
        // simulate and handle responses in success
        // requests associated to WORKFLOW_TYPE_1 have no more step thus they have been deleted
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow1.size() + requestsWorkflow2.size(),
                                                            1,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP),
                                                            requestsWorkflow2.size(),
                                                            RequestStatus.TO_DISPATCH,
                                                            2,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + 1)),
                              false);

        // --> STEP 2
        // simulate and handle responses in success
        // requests associated to WORKFLOW_TYPE_2 have no more step thus they have been deleted (requests in success
        // are deleted)
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow2.size(),
                                                            2,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + 1),
                                                            0,
                                                            RequestStatus.SUCCESS,
                                                            2,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + 2)),
                              false);
    }

    /**
     * The following workflow is executed but not until the end. An ERROR was triggered at STEP1.
     * WORKFLOW_TYPE_2 -> worker 3, worker1 X, (worker2 X)
     */
    @Test
    @Purpose("Verify in requests are properly handled if the workflow is interrupted with a worker response ERROR")
    public void workflow_error_worker_response_error() {
        // -------------
        // --- GIVEN ---
        // -------------
        // create worker requests
        List<Request> requestsWorkflow = createRequests("requestIdWorkflowError-",
                                                        OffsetDateTime.now(),
                                                        WORKFLOW_TYPE_2,
                                                        "sourceWorkflowError",
                                                        "sessionWorkflowError",
                                                        RequestStatus.TO_DISPATCH,
                                                        CONTENT_BODY.getBytes(),
                                                        null,
                                                        2);
        // -------------------
        // --- WHEN / THEN ---
        // -------------------
        // --> STEP 0
        // simulate and handle responses in success
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow.size(),
                                                            0,
                                                            CONTENT_BODY,
                                                            requestsWorkflow.size(),
                                                            RequestStatus.TO_DISPATCH,
                                                            1,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP)), false);
        // --> STEP 1
        // simulate and handle responses in error
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow.size(),
                                                            1,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP),
                                                            requestsWorkflow.size(),
                                                            RequestStatus.ERROR,
                                                            1,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + 1)), true);
    }

    /**
     * The following workflow is executed but not until the end. An ERROR was triggered at STEP1.
     * WORKFLOW_TYPE_2 -> worker3, worker1 X, (worker2 X)
     * <br/>
     * A retry is then programmed to resume the workflow from the step in failure.
     * WORKFLOW_TYPE_2 -> worker3, worker1 (retry in success), worker2
     */
    @Test
    @Purpose("Verify in requests are properly handled if the workflow is interrupted with a worker response ERROR and"
             + " retried in success")
    public void workflow_error_worker_response_error_with_retry_success() {
        // -------------
        // --- GIVEN ---
        // -------------
        // create worker requests
        List<Request> requestsWorkflow = createRequests("requestIdWorkflowErrorWithRetry-",
                                                        OffsetDateTime.now(),
                                                        WORKFLOW_TYPE_2,
                                                        "sourceWorkflowErrorWithRetry",
                                                        "sessionWorkflowErrorWithRetry",
                                                        RequestStatus.TO_DISPATCH,
                                                        CONTENT_BODY.getBytes(),
                                                        null,
                                                        2);
        // -------------------
        // --- WHEN / THEN ---
        // -------------------
        // --> STEP 0
        // simulate and handle responses in success
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow.size(),
                                                            INIT_STEP,
                                                            CONTENT_BODY,
                                                            requestsWorkflow.size(),
                                                            RequestStatus.TO_DISPATCH,
                                                            INIT_STEP + 1,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP)), false);
        // --> STEP 1
        // simulate and handle responses in error
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow.size(),
                                                            INIT_STEP + 1,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP),
                                                            requestsWorkflow.size(),
                                                            RequestStatus.ERROR,
                                                            INIT_STEP + 1,

                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + 1)), true);

        // --> RETRY
        requestService.updateRequestsStatusTo(new PageImpl<>(requestRepository.findAll()), RequestStatus.DISPATCHED);

        // --> STEP 1
        // simulate and handle responses with retry
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow.size(),
                                                            INIT_STEP + 1,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + 1),
                                                            requestsWorkflow.size(),
                                                            RequestStatus.TO_DISPATCH,
                                                            INIT_STEP + 2,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + 1)),
                              false);

        // --> STEP 2
        // simulate and handle responses in success
        // requests associated to WORKFLOW_TYPE_2 have no more step thus they have been deleted (requests in success
        // are deleted)
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow.size(),
                                                            2,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + 1),
                                                            0,
                                                            RequestStatus.SUCCESS,
                                                            2,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + 2)),
                              false);

    }

    // -----------------------------------------------------------------------------------------------------------------

    // --------------------
    // --- INIT METHODS ---
    // --------------------
    private void initWorkers() {
        // save in db
        List<WorkerConfig> workers = List.of(WorkerConfig.build(WORKER_TYPE_1, Set.of(CONTENT_TYPE_1), CONTENT_TYPE_2),
                                             WorkerConfig.build(WORKER_TYPE_2, Set.of(CONTENT_TYPE_2), WORKER_TYPE_3),
                                             WorkerConfig.build(WORKER_TYPE_3, Set.of(CONTENT_TYPE_3), CONTENT_TYPE_1));
        workerConfigRepository.saveAll(workers);
        // register workers in cache
        List<WorkerHeartBeatEvent> workersBeats = List.of(new WorkerHeartBeatEvent(WORKER_TYPE_1,
                                                                                   WORKER_TYPE_1,
                                                                                   OffsetDateTime.now()),
                                                          new WorkerHeartBeatEvent(WORKER_TYPE_2,
                                                                                   WORKER_TYPE_2,
                                                                                   OffsetDateTime.now()),
                                                          new WorkerHeartBeatEvent(WORKER_TYPE_3,
                                                                                   WORKER_TYPE_3,
                                                                                   OffsetDateTime.now()));
        workerCacheService.registerWorkers(workersBeats);
    }

    private void initWorkflows() {
        Workflow workflow1 = new Workflow(WORKFLOW_TYPE_1, List.of(WORKER_TYPE_1, WORKER_TYPE_2));
        Workflow workflow2 = new Workflow(WORKFLOW_TYPE_2, List.of(WORKER_TYPE_3, WORKER_TYPE_1, WORKER_TYPE_2));
        workflowRepository.saveAll(List.of(workflow1, workflow2));
    }

    // ----------------------
    // --- HELPER METHODS ---
    // ----------------------

    private void executeWorkflowsSteps(ExpectedWorkflowIncrement expected, boolean triggerResponseError) {
        requestService.handleRequests(requestRepository.findAll(), new SessionsRequestsInfo(), false);
        assertRequestsUpdate(expected.nbExpectedRequestsAfterHandle,
                             RequestStatus.DISPATCHED,
                             expected.expectedStepAfterHandle,
                             expected.expectedContentAfterHandle);
        // simulate and handle responses
        simulateWorkerResponse(requestRepository.findAll(), expected.expectedStepAfterHandle, triggerResponseError);
        assertRequestsUpdate(expected.nbExpectedRequestsAfterResponse,
                             expected.expectedStatusAfterResponse,
                             expected.expectedStepAfterResponse,
                             expected.expectedContentAfterResponse);
    }

    private void assertRequestsUpdate(int nbExpectedRequests,
                                      RequestStatus expectedRequestStatus,
                                      int expectedStep,
                                      String expectedContent) {
        List<Request> updatedRequests = requestRepository.findAll();
        assertThat(updatedRequests).hasSize(nbExpectedRequests);
        if (nbExpectedRequests != 0) {
            // Check final request status
            assertThat(updatedRequests.stream()
                                      .allMatch(request -> request.getStatus()
                                                                  .equals(expectedRequestStatus))).as(String.format(
                "Expected all Requests to be in status %s",
                expectedRequestStatus)).isTrue();
            // Check final workflow step
            assertThat(updatedRequests.stream()
                                      .allMatch(request -> request.getStep() == expectedStep)).as(String.format(
                "Expected all Requests to be in step %d",
                expectedStep)).isTrue();
            // Check final body content
            assertThat(updatedRequests.stream()
                                      .allMatch(request -> Arrays.equals(request.getContent(),
                                                                         expectedContent.getBytes()))).as(String.format(
                "Expected all Requests to have content \"%s\"",
                expectedContent)).isTrue();

        }
    }

    private void simulateWorkerResponse(List<Request> requests, int expectedStepAfterHandle, boolean triggerError) {
        // simulate worker responses
        List<WorkerResponseEvent> responses = new ArrayList<>();
        for (Request req : requests) {
            WorkerResponseEvent response = new WorkerResponseEvent();
            response.setContent(String.format(CONTENT_BODY_RESPONSE, expectedStepAfterHandle).getBytes());
            if (triggerError) {
                response.setStatus(WorkerResponseStatus.ERROR);
            } else {
                response.setStatus(WorkerResponseStatus.SUCCESS);
            }

            MessageProperties properties = new MessageProperties();
            properties.setHeader(EventHeadersHelper.REQUEST_ID_HEADER, req.getRequestId());
            response.setMessageProperties(properties);
            responses.add(response);
        }
        requestRepository.saveAll(requests);
        requestService.handleWorkersResponses(responses);
    }

    record ExpectedWorkflowIncrement(int nbExpectedRequestsAfterHandle,
                                     int expectedStepAfterHandle,
                                     String expectedContentAfterHandle,
                                     int nbExpectedRequestsAfterResponse,
                                     RequestStatus expectedStatusAfterResponse,
                                     int expectedStepAfterResponse,
                                     String expectedContentAfterResponse) {

    }
}


