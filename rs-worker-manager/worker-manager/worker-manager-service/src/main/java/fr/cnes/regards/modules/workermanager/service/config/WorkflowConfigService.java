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
import fr.cnes.regards.modules.workermanager.domain.config.Workflow;
import fr.cnes.regards.modules.workermanager.dto.WorkflowDto;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.*;

/**
 * Service to save {@link Workflow} from {@link WorkflowDto} configuration
 *
 * @author Iliana Ghazali
 **/
@RegardsTransactional
@Service
public class WorkflowConfigService {

    /**
     * Error status codes
     */
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
     * Save new workflow configurations only if valid
     *
     * @return set of errors. Empty if all configuration are valid.
     */
    public Set<String> importConfiguration(Set<WorkflowDto> workflows) {
        Set<String> errors = new HashSet<>();
        for (WorkflowDto workflow : workflows) {
            // check workflowDto model and consistency
            Errors modelViolations = new MapBindingResult(new HashMap<>(), WorkflowDto.class.getName());
            validator.validate(workflow, modelViolations);
            // import workflow configuration if valid
            if (modelViolations.hasErrors()) {
                errors.addAll(ErrorTranslator.getErrors(modelViolations));
            } else if (isWorkflowValid(workflow, errors)) {
                workflowRepository.save(new Workflow(workflow.getType(), workflow.getWorkerTypes()));
            }
        }
        return errors;
    }

    /**
     * Check if the workflow is valid on 2 conditions :
     * <ul>
     *     <li>all worker configurations of the chain must exist,</li>
     *     <li>contentTypeOut of each workers are chainable</li>
     * </ul>
     */
    private boolean isWorkflowValid(WorkflowDto workflow, Set<String> errors) {
        String lastContentTypeOut = null;
        int step = 0;
        boolean isWorkflowValid = true;

        for (String workerType : workflow.getWorkerTypes()) {
            Optional<WorkerConfig> workerConfig = workerConfigRepository.findByWorkerType(workerType);
            // Check if worker config exists
            if (workerConfig.isEmpty()) {
                errors.add(String.format("""
                                             [%s] %s : worker "%s" configured at step %d does not exist.
                                             You can only configure a worker type that is already registered.""",
                                         NOT_EXISTING_WORKER_ERROR_KEY,
                                         workflow.getType(),
                                         workerType,
                                         step));
                isWorkflowValid = false;
            } else {
                // Check if contentTypeOut is present
                String currentContentTypeOut = workerConfig.get().getContentTypeOutput();
                if (currentContentTypeOut == null) {
                    errors.add(String.format("""
                                                 [%s] %s : contentTypeOut of worker "%s" configured \
                                                 at step %d must be present.
                                                 Make sure all contentTypeOuts of the workflow are configured and chainable.""",
                                             NULL_CONTENT_TYPE_OUT_ERROR_KEY,
                                             workflow.getType(),
                                             workerType,
                                             step));
                    isWorkflowValid = false;
                } // verify if current worker can be chained to the previous one
                else if (step != 0 && !workerConfig.get().getContentTypeInputs().contains(lastContentTypeOut)) {
                    errors.add(String.format("""
                                                 [%s] %s : contentTypeInputs of worker "%s" configured \
                                                 at step %d do not contain the last worker contentTypeOut "%s".
                                                 Make sure each contentTypeOut is managed by the next worker inputs.""",
                                             NOT_CONSISTENT_CONTENT_TYPE_OUT_ERROR_KEY,
                                             workflow.getType(),
                                             workerType,
                                             step,
                                             lastContentTypeOut));
                    isWorkflowValid = false;
                } else {
                    lastContentTypeOut = currentContentTypeOut;
                }
                step++;
            }
        }
        return isWorkflowValid;
    }

    public List<Workflow> findAll() {
        return workflowRepository.findAll();
    }

    public void deleteAllInBatch() {
        workflowRepository.deleteAllInBatch();
    }

}
