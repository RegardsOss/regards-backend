/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.gson.Gson;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.manager.ModuleImportReport;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.workermanager.dao.IWorkerConfigRepository;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.domain.request.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.dto.WorkerConfigDto;
import fr.cnes.regards.modules.workermanager.dto.WorkflowConfigDto;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.config.WorkerManagerConfigManager;
import fr.cnes.regards.modules.workermanager.service.sessions.SessionHelper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static fr.cnes.regards.modules.workermanager.service.flow.RequestHandlerConfiguration.AVAILABLE_WORKER_TYPE_1;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Check that reschedule of {@link Request}s works properly when requests are in
 * {@link RequestStatus#NO_WORKER_AVAILABLE}
 *
 * @author Iliana Ghazali
 **/
@ActiveProfiles(value = { "default", "test", "testAmqp" }, inheritProfiles = false)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=request_handler_workflow_it",
                                   "regards.amqp.enabled=true",
                                   "regards.workermanager.request.bulk.size=1000",
                                   "regards.amqp.enabled=true" },
                    locations = { "classpath:application-test.properties" })
@ContextConfiguration(classes = { RequestHandlerConfiguration.class })
public class RequestScanWorkflowServiceIT extends AbstractWorkerManagerIT {

    private static final String SRC_TEST_RESOURCES_CONFIG = "src/test/resources/config/";

    @Autowired
    private IWorkerConfigRepository workerConfigRepository;

    @Autowired
    private WorkerManagerConfigManager configManager;

    @Autowired
    private Gson gson;

    @Before
    public void init() {
        workerConfigRepository.deleteAll();
    }

    private ModuleImportReport initConfigurations() throws FileNotFoundException {
        WorkflowConfigDto workflowConfig1 = getWorkflowConfigDto("workflow1_conf_nominal.json");
        WorkflowConfigDto workflowConfig2 = getWorkflowConfigDto("workflow2_conf_nominal.json");
        WorkerConfigDto workerConfig1 = getWorkerConfigDto("worker1_conf_nominal.json");
        WorkerConfigDto workerConfig2 = getWorkerConfigDto("worker2_conf_nominal.json");

        return configManager.importConfigurationAndLog(ModuleConfiguration.build(null,
                                                                                 List.of(ModuleConfigurationItem.build(
                                                                                             workflowConfig1),
                                                                                         ModuleConfigurationItem.build(
                                                                                             workflowConfig2),
                                                                                         ModuleConfigurationItem.build(
                                                                                             workerConfig1),
                                                                                         ModuleConfigurationItem.build(
                                                                                             workerConfig2))));
    }

    @Test
    @Purpose("Retry successfully workflow requests in NO_WORKER_AVAILABLE")
    public void retry_workflow_requests_nominal() throws FileNotFoundException {
        // GIVEN
        // import requests
        List<Request> requests = new ArrayList<>();
        requests.add(createRequest(UUID.randomUUID().toString(),
                                   RequestStatus.NO_WORKER_AVAILABLE,
                                   "workflowType1",
                                   2,
                                   "workerType1"));
        requests.add(createRequest(UUID.randomUUID().toString(),
                                   RequestStatus.NO_WORKER_AVAILABLE,
                                   "workflowType2",
                                   1,
                                   "workerType1"));
        requestRepository.saveAll(requests);

        // WHEN
        // Simulate new conf for worker. So request in status NO_WORKER_AVAILABLE can be sent
        // import configurations and schedule retry job
        assertThat(initConfigurations().getImportErrors()).isEmpty();
        requestService.scheduleRequestRetryJob(new SearchRequestParameters());

        // THEN
        assertThat(waitForRequests(2, RequestStatus.DISPATCHED, 10, TimeUnit.SECONDS)).isTrue();

        // Wait for all session properties update received :
        // -2 NO_WORKER_AVAILABLE
        //  0 ERROR
        // 0 INVALID_CONTENT
        // +2 TO_DISPATCH
        // -2 TO_DISPATCH
        // 2 DISPATCHED
        waitForSessionProperties(2, 10, TimeUnit.SECONDS);
        SessionHelper.checkSession(stepPropertyUpdateRepository,
                                   DEFAULT_SOURCE,
                                   DEFAULT_SESSION,
                                   AVAILABLE_WORKER_TYPE_1,
                                   0,
                                   0,
                                   -2,
                                   2,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0);

        // Check interrupted step
        checkFinalStates(requests);
    }

    private void checkFinalStates(List<Request> requests) {
        requestRepository.findAll().forEach(request -> {
            Optional<Request> requestDispatchedOpt = requests.stream()
                                                             .filter(initReq -> Objects.equals(initReq.getRequestId(),
                                                                                               request.getRequestId()))
                                                             .findFirst();
            assertThat(requestDispatchedOpt.isPresent()).isTrue();
            Request requestDispatched = requestDispatchedOpt.get();
            assertThat(requestDispatched.getStepNumber()).isEqualTo(request.getStepNumber());
            assertThat(requestDispatched.getStepWorkerType()).isEqualTo(request.getStepWorkerType());
        });

    }

    private WorkflowConfigDto getWorkflowConfigDto(String workflowConfig) throws FileNotFoundException {
        return gson.fromJson(new FileReader(SRC_TEST_RESOURCES_CONFIG + workflowConfig), WorkflowConfigDto.class);
    }

    private WorkerConfigDto getWorkerConfigDto(String workerConfig) throws FileNotFoundException {
        return gson.fromJson(new FileReader(SRC_TEST_RESOURCES_CONFIG + workerConfig), WorkerConfigDto.class);
    }

}
