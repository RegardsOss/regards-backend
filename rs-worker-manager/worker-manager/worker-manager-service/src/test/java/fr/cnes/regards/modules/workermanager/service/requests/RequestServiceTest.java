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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.workermanager.dao.IRequestRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowConfig;
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowStep;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.cache.WorkerCacheService;
import fr.cnes.regards.modules.workermanager.service.sessions.SessionService;
import fr.cnes.regards.modules.workermanager.service.sessions.SessionsRequestsInfo;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tests for {@link RequestService}
 * <p>
 * WARNING : not all the class methods are tested. This test has been added following the
 * DM104 for workers workflow management.
 * //FIXME enhance test covering
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class RequestServiceTest {

    public static final String REQUEST_ID_WITH_WORKER_1 = "requestIdWithWorker";

    public static final String REQUEST_ID_WITH_WORKFLOW_1 = "requestIdWithWorkflow";

    public static final String CONTENT_TYPE_WORKER_1 = "contentTypeWorker1";

    public static final String WORKFLOW_TYPE_1 = "workflow1";

    public static final String WORKER_TYPE_1 = "workerType1";

    public static final String WORKER_TYPE_2 = "workerType2";

    public static final Integer INIT_STEP = 1;

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @InjectMocks
    private RequestService requestService;

    @Mock
    private IRequestRepository requestRepository;

    @Mock
    private WorkerCacheService workerCacheService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Mock
    @SuppressWarnings("unused")
    private SessionService sessionService;

    @Mock
    @SuppressWarnings("unused")
    private IPublisher publisher;

    @Before
    public void init() {
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn("defaultTenant");
        ReflectionTestUtils.setField(requestService, "WORKER_REQUEST_QUEUE_NAME_TEMPLATE", "regards.worker.%s.request");
        ReflectionTestUtils.setField(requestService, "ROUTING_KEY", "#");
    }

    @Test
    @Purpose("Test if requests with or without workflow are properly dispatched")
    @SuppressWarnings("unchecked")
    public void handle_requests_nominal() {
        // --- GIVEN ---
        List<Request> requests = initRequests();
        // Worker is directly available in cache for the request without workflow
        Mockito.when(workerCacheService.getWorkerTypeByContentType(CONTENT_TYPE_WORKER_1))
               .thenReturn(Optional.of(WORKER_TYPE_1));
        // Worker will be retrieved from the workflow for the request with workflow
        Mockito.when(workerCacheService.getWorkerTypeByContentType(WORKFLOW_TYPE_1)).thenReturn(Optional.empty());
        WorkflowConfig workflowConfig = new WorkflowConfig(WORKFLOW_TYPE_1,
                                                           List.of(new WorkflowStep(INIT_STEP, WORKER_TYPE_2)));
        Mockito.when(workflowService.findWorkflowByType(WORKFLOW_TYPE_1)).thenReturn(Optional.of(workflowConfig));
        Mockito.when(workflowService.getWorkerTypeInWorkflow(workflowConfig, INIT_STEP))
               .thenReturn(Optional.of(WORKER_TYPE_2));
        Mockito.when(workerCacheService.isWorkerTypeInCache(WORKER_TYPE_2)).thenReturn(true);

        // --- WHEN ---
        requestService.handleRequests(requests, new SessionsRequestsInfo(), false);

        // --- THEN ---
        // Requests are correctly saved with dipatched status
        ArgumentCaptor<List<Request>> requestsUpdatedCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(requestRepository).saveAll(requestsUpdatedCaptor.capture());
        checkRequestsDispatched(requestsUpdatedCaptor.getValue());

    }

    @Test
    @Purpose("Test if requests are not dispatched if corresponding workers were not found.")
    @SuppressWarnings("unchecked")
    public void handle_requests_error_worker_not_found() {
        // --- GIVEN ---
        List<Request> requests = initRequests();

        // --- WHEN ---
        requestService.handleRequests(requests, new SessionsRequestsInfo(), false);

        // --- THEN ---
        // Requests are correctly saved with dipatched status
        ArgumentCaptor<List<Request>> requestsUpdatedCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(requestRepository).saveAll(requestsUpdatedCaptor.capture());
        checkRequestsNotDispatched(requestsUpdatedCaptor.getValue());

    }

    private void checkRequestsDispatched(List<Request> actualRequestsUpdated) {
        // Request with worker directly available
        Request requestWithWorker = actualRequestsUpdated.get(0);
        Assertions.assertThat(requestWithWorker.getStatus()).isEqualTo(RequestStatus.DISPATCHED);
        Assertions.assertThat(requestWithWorker.getDispatchedWorkerType()).isEqualTo(WORKER_TYPE_1);
        // Request with workflow of workers
        Request requestWithWorkflow = actualRequestsUpdated.get(1);
        Assertions.assertThat(requestWithWorkflow.getStatus()).isEqualTo(RequestStatus.DISPATCHED);
        Assertions.assertThat(requestWithWorkflow.getDispatchedWorkerType()).isEqualTo(WORKER_TYPE_2);
    }

    private void checkRequestsNotDispatched(List<Request> actualRequestsUpdated) {
        // Request with worker directly available
        Request requestWithWorker = actualRequestsUpdated.get(0);
        Assertions.assertThat(requestWithWorker.getStatus()).isEqualTo(RequestStatus.NO_WORKER_AVAILABLE);
        Assertions.assertThat(requestWithWorker.getDispatchedWorkerType()).isEqualTo(null);
        // Request with workflow of workers
        Request requestWithWorkflow = actualRequestsUpdated.get(1);
        Assertions.assertThat(requestWithWorkflow.getStatus()).isEqualTo(RequestStatus.NO_WORKER_AVAILABLE);
        Assertions.assertThat(requestWithWorkflow.getDispatchedWorkerType()).isEqualTo(null);
    }

    private List<Request> initRequests() {
        LOGGER.info("=========================> END INIT REQUESTS FOR TESTS  <=====================");
        // requests without workflow
        List<Request> createdRequests = new ArrayList<>();

        createdRequests.add(createRequest(REQUEST_ID_WITH_WORKER_1,
                                          OffsetDateTime.now(),
                                          CONTENT_TYPE_WORKER_1,
                                          "sourceWorker",
                                          "sessionWorker",
                                          "lorem ipsum sine workflow".getBytes()));

        // requests with workflow
        createdRequests.add(createRequest(REQUEST_ID_WITH_WORKFLOW_1,
                                          OffsetDateTime.now(),
                                          WORKFLOW_TYPE_1,
                                          "sourceWorkflow",
                                          "sessionWorkflow",
                                          "lorem ipsum cum workflow 1".getBytes()));

        LOGGER.info("=========================> END INIT FOR TESTS  <=====================");
        return createdRequests;
    }

    protected Request createRequest(String requestId,
                                    OffsetDateTime creationDate,
                                    String contentType,
                                    String source,
                                    String session,
                                    byte[] content) {
        Request request = new Request();
        request.setRequestId(requestId);
        request.setCreationDate(creationDate);
        request.setContentType(contentType);
        request.setSource(source);
        request.setSession(session);
        request.setStatus(RequestStatus.TO_DISPATCH);
        request.setContent(content);
        request.setStep(INIT_STEP);
        return request;
    }

}
