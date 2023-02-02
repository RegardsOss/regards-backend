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
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowConfig;
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowStep;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.cache.AbstractWorkerManagerServiceUtilsIT;
import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.*;

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

    private static final int STEP_INC = 10;

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
        List<Message> requestsWorkflow1 = createRequestEvents("requestIdWorkflowNominal1-",
                                                              WORKFLOW_TYPE_1,
                                                              "sourceWorkflowNominal1",
                                                              "sessionWorkflowNominal1",
                                                              CONTENT_BODY.getBytes(),
                                                              2);
        List<Message> requestsWorkflow2 = createRequestEvents("requestIdWorkflowNominal2-",
                                                              WORKFLOW_TYPE_2,
                                                              "sourceWorkflowNominal2",
                                                              "sessionWorkflowNominal2",
                                                              CONTENT_BODY.getBytes(),
                                                              2);

        // -------------------
        // ------  WHEN ------
        // -------------------
        requestService.registerRequests(requestsWorkflow1);
        requestService.registerRequests(requestsWorkflow2);

        // -------------------
        // ------  THEN ------
        // -------------------
        // --> STEP 0
        // simulate and handle responses in success
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow1.size() + requestsWorkflow2.size(),
                                                            INIT_STEP,
                                                            CONTENT_BODY,
                                                            requestsWorkflow1.size() + requestsWorkflow2.size(),
                                                            RequestStatus.DISPATCHED,
                                                            INIT_STEP + STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP)), false);

        // --> STEP 1
        // simulate and handle responses in success
        // requests associated to WORKFLOW_TYPE_1 have no more step thus they have been deleted
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow1.size() + requestsWorkflow2.size(),
                                                            INIT_STEP + STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP),
                                                            requestsWorkflow2.size(),
                                                            RequestStatus.DISPATCHED,
                                                            INIT_STEP + 2 * STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + STEP_INC)),
                              false);

        // --> STEP 2
        // simulate and handle responses in success
        // requests associated to WORKFLOW_TYPE_2 have no more step thus they have been deleted (requests in success
        // are deleted)
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow2.size(),
                                                            INIT_STEP + 2 * STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + STEP_INC),
                                                            0,
                                                            RequestStatus.SUCCESS,
                                                            INIT_STEP + 2 * STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE,
                                                                          INIT_STEP + 2 * STEP_INC)), false);
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
        List<Message> requestsWorkflow = createRequestEvents("requestIdWorkflowError-",
                                                             WORKFLOW_TYPE_2,
                                                             "sourceWorkflowError",
                                                             "sessionWorkflowError",
                                                             CONTENT_BODY.getBytes(),
                                                             2);

        // -------------------
        // ------  WHEN ------
        // -------------------
        requestService.registerRequests(requestsWorkflow);

        // -------------------
        // ------  THEN ------
        // -------------------
        // --> STEP 0
        // simulate and handle responses in success
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow.size(),
                                                            INIT_STEP,
                                                            CONTENT_BODY,
                                                            requestsWorkflow.size(),
                                                            RequestStatus.DISPATCHED,
                                                            INIT_STEP + STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP)), false);
        // --> STEP 1
        // simulate and handle responses in error
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow.size(),
                                                            INIT_STEP + STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP),
                                                            requestsWorkflow.size(),
                                                            RequestStatus.ERROR,
                                                            INIT_STEP + STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP)), true);
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
        List<Message> requestsWorkflow = createRequestEvents("requestIdWorkflowErrorWithRetry-",
                                                             WORKFLOW_TYPE_2,
                                                             "sourceWorkflowErrorWithRetry",
                                                             "sessionWorkflowErrorWithRetry",
                                                             CONTENT_BODY.getBytes(),
                                                             2);

        // -------------------
        // ------  WHEN ------
        // -------------------
        requestService.registerRequests(requestsWorkflow);

        // -------------------
        // ------  THEN ------
        // -------------------
        // --> STEP 0
        // simulate and handle responses in success
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow.size(),
                                                            INIT_STEP,
                                                            CONTENT_BODY,
                                                            requestsWorkflow.size(),
                                                            RequestStatus.DISPATCHED,
                                                            INIT_STEP + STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP)), false);
        // --> STEP 1
        // simulate and handle responses in error
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow.size(),
                                                            INIT_STEP + STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP),
                                                            requestsWorkflow.size(),
                                                            RequestStatus.ERROR,
                                                            INIT_STEP + STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP)), true);

        // --> RETRY
        requestService.updateRequestsStatusTo(new PageImpl<>(requestRepository.findAll()), RequestStatus.DISPATCHED);

        // --> STEP 1
        // simulate and handle responses with retry
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow.size(),
                                                            INIT_STEP + STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP),
                                                            requestsWorkflow.size(),
                                                            RequestStatus.DISPATCHED,
                                                            INIT_STEP + 2 * STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + STEP_INC)),
                              false);

        // --> STEP 2
        // simulate and handle responses in success
        // requests associated to WORKFLOW_TYPE_2 have no more step thus they have been deleted (requests in success
        // are deleted)
        executeWorkflowsSteps(new ExpectedWorkflowIncrement(requestsWorkflow.size(),
                                                            INIT_STEP + 2 * STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE, INIT_STEP + STEP_INC),
                                                            0,
                                                            RequestStatus.SUCCESS,
                                                            INIT_STEP + 2 * STEP_INC,
                                                            String.format(CONTENT_BODY_RESPONSE,
                                                                          INIT_STEP + 2 * STEP_INC)), false);

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
        WorkflowConfig workflowConfig1 = new WorkflowConfig(WORKFLOW_TYPE_1,
                                                            List.of(new WorkflowStep(INIT_STEP + STEP_INC,
                                                                                     WORKER_TYPE_2),
                                                                    new WorkflowStep(INIT_STEP, WORKER_TYPE_1)));
        WorkflowConfig workflowConfig2 = new WorkflowConfig(WORKFLOW_TYPE_2,
                                                            List.of(new WorkflowStep(INIT_STEP + STEP_INC,
                                                                                     WORKER_TYPE_1),
                                                                    new WorkflowStep(INIT_STEP + 2 * STEP_INC,
                                                                                     WORKER_TYPE_2),
                                                                    new WorkflowStep(INIT_STEP, WORKER_TYPE_3)));
        workflowRepository.saveAll(List.of(workflowConfig1, workflowConfig2));
    }

    // ----------------------
    // --- HELPER METHODS ---
    // ----------------------

    private void executeWorkflowsSteps(ExpectedWorkflowIncrement expected, boolean triggerResponseError) {
        // check request states after previous handle
        assertRequestsUpdate(expected.nbExpectedRequestsAfterHandle,
                             RequestStatus.DISPATCHED,
                             expected.expectedStepAfterHandle,
                             expected.expectedContentAfterHandle);
        // simulate workerResponse
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
                                      .allMatch(request -> Objects.requireNonNull(request.getStep())
                                                           == expectedStep)).as(String.format(
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


