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
package fr.cnes.regards.modules.workermanager.service.config;

import com.google.gson.Gson;
import fr.cnes.regards.framework.gson.GsonCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.workermanager.dao.IWorkerConfigRepository;
import fr.cnes.regards.modules.workermanager.dao.IWorkflowRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerConfig;
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowConfig;
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowStep;
import fr.cnes.regards.modules.workermanager.dto.WorkflowConfigDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Validator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test import configurations for {@link WorkflowConfigService}
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class WorkflowConfigConfigServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowConfigConfigServiceTest.class);

    /**
     * Error status codes
     */
    private static final String DUPLICATED_STEP_NUMBERS_ERROR_KEY = "DuplicatedStepNumbersError";

    private static final String DUPLICATED_WORKFLOW_TYPES_ERROR_KEY = "DuplicatedWorkflowTypesError";

    private static final String NOT_EXISTING_WORKER_ERROR_KEY = "NotExistingWorkerError";

    private static final String NULL_CONTENT_TYPE_OUT_ERROR_KEY = "NullContentTypeOutError";

    private static final String NOT_CONSISTENT_CONTENT_TYPE_OUT_ERROR_KEY = "NonConsistentContentTypeOutError";

    /**
     * Testing environment
     */
    private static final String WORKFLOW_1 = "workflow1";

    private static final String WORKFLOW_2 = "workflow2";

    private static final String WORKER_1 = "worker1";

    private static final String WORKER_2 = "worker2";

    private static final String CONTENT_TYPE_1 = "contentType1";

    private static final String CONTENT_TYPE_2 = "contentType2";

    private static final String CONTENT_TYPE_3 = "contentType3";

    private static final String SRC_TEST_RESOURCES_CONFIG = "src/test/resources/config/";

    private final Gson gson = GsonCustomizer.gsonBuilder(Optional.empty(), Optional.empty()).create();

    @InjectMocks
    private WorkflowConfigService workflowConfigService;

    @Mock
    private IWorkflowRepository workflowRepository;

    @Mock
    private IWorkerConfigRepository workerConfigRepository;

    @Spy
    private Validator validator;

    @Test
    @Purpose("Verify workflows can be created from valid workflowDtos.")
    public void import_conf_nominal() throws FileNotFoundException {
        // --- GIVEN ---
        Set<WorkflowConfigDto> workflowDtos = Set.of(getWorkflowDto("workflow1_conf_nominal.json"),
                                                     getWorkflowDto("workflow2_conf_nominal.json"));

        // mock workerConfigs to validate workflow
        Mockito.when(workerConfigRepository.findByWorkerType(WORKER_1))
               .thenReturn(Optional.of(WorkerConfig.build("type1", Set.of(CONTENT_TYPE_1), CONTENT_TYPE_2)));
        Mockito.when(workerConfigRepository.findByWorkerType(WORKER_2))
               .thenReturn(Optional.of(WorkerConfig.build("type2", Set.of(CONTENT_TYPE_2), CONTENT_TYPE_3)));

        // --- WHEN ---
        Set<String> errors = workflowConfigService.importConfiguration(workflowDtos);

        // --- THEN ---
        Mockito.verify(workflowRepository)
               .save(new WorkflowConfig(WORKFLOW_1,
                                        List.of(new WorkflowStep(1, WORKER_1), new WorkflowStep(2, WORKER_2))));
        Mockito.verify(workflowRepository).save(new WorkflowConfig(WORKFLOW_2, List.of(new WorkflowStep(1, WORKER_1))));
        assertThat(errors).isEmpty();
    }

    @Test
    @Purpose("Verify workflows are not created when same worker types exist.")
    public void import_conf_error_duplicated_workflow_types() throws FileNotFoundException {
        // --- GIVEN ---
        Set<WorkflowConfigDto> workflowDtos = Set.of(getWorkflowDto("workflow_conf_error_duplicated_types_1.json"),
                                                     getWorkflowDto("workflow_conf_error_duplicated_types_2.json"));

        Set<String> duplicatedWorkflowTypes = workflowDtos.stream()
                                                          .map(WorkflowConfigDto::getWorkflowType)
                                                          .collect(Collectors.toUnmodifiableSet());
        Mockito.when(workerConfigRepository.findAllByWorkerTypeIn(duplicatedWorkflowTypes))
               .thenReturn(duplicatedWorkflowTypes);

        // --- WHEN ---
        Set<String> errors = workflowConfigService.importConfiguration(workflowDtos);

        // --- THEN ---
        assertThat(errors).hasSize(1);
        assertThat(errors.stream().allMatch(error -> error.contains(DUPLICATED_WORKFLOW_TYPES_ERROR_KEY))).isTrue();
        LOGGER.error("Expected errors during the test {}", errors);
    }

    @Test
    @Purpose("Verify workflows are not created if associated workers do not exist.")
    public void import_conf_error_not_existing_workers() throws FileNotFoundException {
        // --- GIVEN ---
        Set<WorkflowConfigDto> workflowDtos = Set.of(getWorkflowDto("workflow1_conf_nominal.json"),
                                                     getWorkflowDto("workflow2_conf_nominal.json"));

        // --- WHEN ---
        Set<String> errors = workflowConfigService.importConfiguration(workflowDtos);

        // --- THEN ---
        Mockito.verifyNoInteractions(workflowRepository);
        assertThat(errors).hasSize(3);
        assertThat(errors.stream().allMatch(error -> error.contains(NOT_EXISTING_WORKER_ERROR_KEY))).isTrue();
        LOGGER.error("Expected errors during the test {}", errors);
    }

    @Test
    @Purpose("Verify workflows are not created if associated workers are not chainable")
    public void import_conf_error_not_chainable_workers() throws FileNotFoundException {
        // --- GIVEN ---
        Set<WorkflowConfigDto> workflowDto = Set.of(getWorkflowDto("workflow1_conf_nominal.json"));

        // mock workerConfigs to invalidate workflow
        Mockito.when(workerConfigRepository.findByWorkerType(WORKER_1))
               .thenReturn(Optional.of(WorkerConfig.build("type1", Set.of(CONTENT_TYPE_1), CONTENT_TYPE_2)));
        Mockito.when(workerConfigRepository.findByWorkerType(WORKER_2))
               .thenReturn(Optional.of(WorkerConfig.build("type2", Set.of(CONTENT_TYPE_3), CONTENT_TYPE_3)));

        // --- WHEN ---
        Set<String> errors = workflowConfigService.importConfiguration(workflowDto);

        // --- THEN ---
        Mockito.verifyNoInteractions(workflowRepository);
        assertThat(errors).hasSize(1);
        assertThat(errors.stream()
                         .allMatch(error -> error.contains(NOT_CONSISTENT_CONTENT_TYPE_OUT_ERROR_KEY))).isTrue();
        LOGGER.error("Expected errors during the test {}", errors);
    }

    @Test
    @Purpose("Verify workflows are not created if associated workers contain null contentTypeOut")
    public void import_conf_error_contentTypeOut_null() throws FileNotFoundException {
        // --- GIVEN ---
        Set<WorkflowConfigDto> workflowDto = Set.of(getWorkflowDto("workflow1_conf_nominal.json"));

        // mock workerConfigs to invalidate workflow
        Mockito.when(workerConfigRepository.findByWorkerType(WORKER_1))
               .thenReturn(Optional.of(WorkerConfig.build("type1", Set.of(CONTENT_TYPE_1), CONTENT_TYPE_2)));
        Mockito.when(workerConfigRepository.findByWorkerType(WORKER_2))
               .thenReturn(Optional.of(WorkerConfig.build("type2", Set.of(CONTENT_TYPE_2), null)));

        // --- WHEN ---
        Set<String> errors = workflowConfigService.importConfiguration(workflowDto);

        // --- THEN ---
        Mockito.verifyNoInteractions(workflowRepository);
        assertThat(errors).hasSize(1);
        assertThat(errors.stream().allMatch(error -> error.contains(NULL_CONTENT_TYPE_OUT_ERROR_KEY))).isTrue();
        LOGGER.error("Expected errors during the test {}", errors);
    }

    @Test
    @Purpose("Verify workflows are not created if step numbers are duplicated")
    public void import_conf_error_duplicated_step_numbers() throws FileNotFoundException {
        // --- GIVEN ---
        Set<WorkflowConfigDto> workflowDto = Set.of(getWorkflowDto("workflow_conf_error_duplicated_steps.json"));

        // --- WHEN ---
        Set<String> errors = workflowConfigService.importConfiguration(workflowDto);

        // --- THEN ---
        Mockito.verifyNoInteractions(workflowRepository);
        assertThat(errors).hasSize(1);
        assertThat(errors.stream().allMatch(error -> error.contains(DUPLICATED_STEP_NUMBERS_ERROR_KEY))).isTrue();
        LOGGER.error("Expected errors during the test {}", errors);
    }

    private WorkflowConfigDto getWorkflowDto(String workflow) throws FileNotFoundException {
        return gson.fromJson(new FileReader(SRC_TEST_RESOURCES_CONFIG + workflow), WorkflowConfigDto.class);
    }

}
