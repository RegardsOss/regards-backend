/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowConfig;
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowStep;
import fr.cnes.regards.modules.workermanager.service.config.WorkflowConfigCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

/**
 * Service to handle {@link WorkflowConfig}
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class WorkflowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowService.class);

    private final WorkflowConfigCacheService workflowConfigCache;

    public WorkflowService(WorkflowConfigCacheService workflowConfigCache) {
        this.workflowConfigCache = workflowConfigCache;
    }

    /**
     * Find specific workerType on a workflow with index.
     *
     * @param workflowConfig    containing workerType to find
     * @param currentStepNumber workerType position to retrieve
     * @return optional workerType.
     */
    public Optional<String> getWorkerTypeInWorkflow(WorkflowConfig workflowConfig, int currentStepNumber) {
        return workflowConfig.getSteps()
                             .stream()
                             .filter(workflowStep -> workflowStep.getStepNumber() == currentStepNumber)
                             .map(WorkflowStep::getWorkerType)
                             .findFirst();
    }

    public Optional<WorkflowConfig> findWorkflowByType(String workflowType) {
        return workflowConfigCache.getWorkflowConfig(workflowType);
    }

    /**
     * Find the current step to execute in a workflow.
     *
     * @param workflowConfig    workflow of steps to execute
     * @param currentStepNumber number of the current step
     * @return index of current step to execute if found
     */
    public OptionalInt getCurrentWorkflowStepIndex(WorkflowConfig workflowConfig, int currentStepNumber) {
        // sort list to guarantee step order
        List<WorkflowStep> steps = getSortedWorkflowSteps(workflowConfig);
        // get current step index
        return IntStream.range(0, steps.size())
                        .filter(stepInd -> steps.get(stepInd).getStepNumber() == currentStepNumber)
                        .findFirst();

    }

    /**
     * Find the next step to execute in a workflow. Empty if workflow is completed.
     *
     * @param workflowConfig    workflow of steps to execute
     * @param currentStepNumber number of the current step
     * @return index of next step to execute if found
     */
    public OptionalInt getNextWorkflowStepIndex(WorkflowConfig workflowConfig, int currentStepNumber) {
        // sort list to guarantee step order
        List<WorkflowStep> steps = getSortedWorkflowSteps(workflowConfig);
        // get current step index
        return IntStream.range(0, steps.size() - 1)
                        .filter(stepInd -> steps.get(stepInd).getStepNumber() == currentStepNumber)
                        .map(nextStepInd -> nextStepInd + 1)
                        .findFirst();
    }

    /**
     * Get first step of a workflow. A workflow has necessarily at least one step.
     *
     * @param workflowConfig workflow of steps to execute
     * @return first workflow step
     */
    public WorkflowStep getFirstStep(WorkflowConfig workflowConfig) {
        // sort list to guarantee step order
        List<WorkflowStep> steps = getSortedWorkflowSteps(workflowConfig);
        return steps.get(0);
    }

    /**
     * Sort workflow steps by ascending step number
     */
    private List<WorkflowStep> getSortedWorkflowSteps(WorkflowConfig workflowConfig) {
        List<WorkflowStep> steps = workflowConfig.getSteps();
        steps.sort(Comparator.comparingInt(WorkflowStep::getStepNumber));
        return steps;
    }

}
