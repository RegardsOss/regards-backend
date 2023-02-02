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

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.modules.workermanager.dao.IWorkerConfigRepository;
import fr.cnes.regards.modules.workermanager.dao.IWorkflowRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerConfig;
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowConfig;
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowStep;
import fr.cnes.regards.modules.workermanager.dto.WorkflowConfigDto;
import fr.cnes.regards.modules.workermanager.dto.WorkflowStepDto;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.*;

/**
 * Service to save {@link WorkflowConfig} from {@link WorkflowConfigDto} configuration
 *
 * @author Iliana Ghazali
 **/
@RegardsTransactional
@Service
public class WorkflowConfigService {

    /**
     * Error status codes
     */
    private static final String DUPLICATED_STEP_NUMBER_ERROR_KEY = "DuplicatedStepNumber";

    private static final String NOT_EXISTING_WORKER_ERROR_KEY = "NotExistingWorkerError";

    private static final String NULL_CONTENT_TYPE_OUT_ERROR_KEY = "NullContentTypeOutError";

    private static final String NOT_CONSISTENT_CONTENT_TYPE_OUT_ERROR_KEY = "NonConsistentContentTypeOutError";

    /**
     * Resources
     */
    private final IWorkflowRepository workflowRepository;

    private final IWorkerConfigRepository workerConfigRepository;

    private final Validator validator;

    public WorkflowConfigService(IWorkflowRepository workflowRepository,
                                 IWorkerConfigRepository workerConfigRepository,
                                 Validator validator) {
        this.workflowRepository = workflowRepository;
        this.workerConfigRepository = workerConfigRepository;
        this.validator = validator;
    }

    /**
     * Save new workflowConfig configurations only if valid
     *
     * @return set of errors. Empty if all configuration are valid.
     */
    public Set<String> importConfiguration(Set<WorkflowConfigDto> workflowConfigDtos) {
        Set<String> errors = new HashSet<>();
        for (WorkflowConfigDto workflowConfigDto : workflowConfigDtos) {
            // check workflowDto model and consistency
            Errors modelViolations = new MapBindingResult(new HashMap<>(), WorkflowConfigDto.class.getName());
            validator.validate(workflowConfigDto, modelViolations);
            // import workflowConfigDto configuration if valid
            if (modelViolations.hasErrors()) {
                errors.addAll(ErrorTranslator.getErrors(modelViolations));
            } else {
                // sort workflow steps by step numbers to guarantee order
                workflowConfigDto.getSteps().sort(Comparator.comparingInt(WorkflowStepDto::getStep));
                // check if workflow is valid
                if (areStepNumbersUnique(workflowConfigDto, errors) && areWorkflowStepContentsValid(workflowConfigDto,
                                                                                                    errors)) {
                    List<WorkflowStep> steps = workflowConfigDto.getSteps()
                                                                .stream()
                                                                .map(stepDto -> new WorkflowStep(stepDto.getStep(),
                                                                                                 stepDto.getWorkerType()))
                                                                .toList();
                    // import valid workflow configuration
                    workflowRepository.save(new WorkflowConfig(workflowConfigDto.getWorkflowType(), steps));
                }
            }

        }
        return errors;
    }

    /**
     * Check if all step numbers of workflow are unique
     */
    private boolean areStepNumbersUnique(WorkflowConfigDto workflowConfigDto, Set<String> errors) {
        boolean isValid = workflowConfigDto.getSteps().stream().map(WorkflowStepDto::getStep).distinct().count()
                          == workflowConfigDto.getSteps().size();
        if (!isValid) {
            errors.add(String.format("[%s - %s]: step duplications detected! Make sure all step numbers are unique!",
                                     DUPLICATED_STEP_NUMBER_ERROR_KEY,
                                     workflowConfigDto.getWorkflowType()));
        }
        return isValid;
    }

    /**
     * Check if workflowConfigDto steps are all valid :
     * <ul>
     *     <li>all worker configurations of the chain must exist,</li>
     *     <li>contentTypeOut of each workers are chainable</li>
     * </ul>
     */
    private boolean areWorkflowStepContentsValid(WorkflowConfigDto workflowConfig, Set<String> errors) {
        String lastContentTypeOut = null;
        boolean isValid = true;
        int stepPos = 0;

        for (WorkflowStepDto workflowStepDto : workflowConfig.getSteps()) {
            Optional<WorkerConfig> workerConfig = workerConfigRepository.findByWorkerType(workflowStepDto.getWorkerType());
            // Check if worker config exists
            if (workerConfig.isEmpty()) {
                errors.add(String.format("""
                                             [%s - %s]: worker "%s" configured at step %d does not exist.
                                             You can only configure a worker type that is already registered.""",
                                         NOT_EXISTING_WORKER_ERROR_KEY,
                                         workflowConfig.getWorkflowType(),
                                         workflowStepDto.getWorkerType(),
                                         workflowStepDto.getStep()));
                isValid = false;
            } else {
                // Check if contentTypeOut is present
                String currentContentTypeOut = workerConfig.get().getContentTypeOutput();
                if (currentContentTypeOut == null) {
                    errors.add(String.format("""
                                                 [%s - %s]: contentTypeOut of worker "%s" configured \
                                                 at step %d must be present.
                                                 Make sure all contentTypeOuts of the workflowConfig are configured and chainable.""",
                                             NULL_CONTENT_TYPE_OUT_ERROR_KEY,
                                             workflowConfig.getWorkflowType(),
                                             workflowStepDto.getWorkerType(),
                                             workflowStepDto.getStep()));
                    isValid = false;
                } // verify if current worker can be chained to the previous one
                else if (stepPos != 0 && !workerConfig.get().getContentTypeInputs().contains(lastContentTypeOut)) {
                    errors.add(String.format("""
                                                 [%s - %s]: contentTypeInputs of worker "%s" configured \
                                                 at step %d do not contain the last worker contentTypeOut "%s".
                                                 Make sure each contentTypeOut is managed by the next worker inputs.""",
                                             NOT_CONSISTENT_CONTENT_TYPE_OUT_ERROR_KEY,
                                             workflowConfig.getWorkflowType(),
                                             workflowStepDto.getWorkerType(),
                                             workflowStepDto.getStep(),
                                             lastContentTypeOut));
                    isValid = false;
                } else {
                    lastContentTypeOut = currentContentTypeOut;
                }
            }
            stepPos++;
        }
        return isValid;
    }

    public List<WorkflowConfig> findAll() {
        return workflowRepository.findAll();
    }

    public void deleteAllInBatch() {
        workflowRepository.deleteAllInBatch();
    }

}
