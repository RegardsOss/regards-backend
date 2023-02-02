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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.workermanager.dao.IWorkflowRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowConfig;
import fr.cnes.regards.modules.workermanager.domain.config.WorkflowStep;
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

    private final IWorkflowRepository workflowRepository;

    public WorkflowService(IWorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    /**
     * Find specific workerType on a workflow with index.
     *
     * @param workflowConfig containing workerType to find
     * @param step           workerType position to retrieve
     * @return optional workerType.
     */
    public Optional<String> getWorkerTypeInWorkflow(WorkflowConfig workflowConfig, Integer step) {
        return workflowConfig.getSteps()
                             .stream()
                             .filter(workflowStep -> workflowStep.getStep() == step)
                             .map(WorkflowStep::getWorkerType)
                             .findFirst();
    }

    public Optional<WorkflowConfig> findWorkflowByType(String workflowType) {
        return workflowRepository.findById(workflowType);
    }

    /**
     * Find the next step to execute in a workflow. Empty if workflow is completed.
     *
     * @param workflowConfig workflow of steps to execute
     * @param step           number of the current step
     * @return index of next step to execute if found
     */
    public OptionalInt getNextWorkflowStepIndex(WorkflowConfig workflowConfig, Integer step) {
        // sort list to guarantee step order
        List<WorkflowStep> steps = getSortedWorkflowSteps(workflowConfig);
        // get next step index
        return IntStream.range(0, steps.size() - 1)
                        .filter(stepInd -> steps.get(stepInd).getStep() == step)
                        .map(currentIndex -> currentIndex + 1)
                        .findFirst();

    }

    /**
     * Find the first step of workflow.
     */
    public int getFirstStep(WorkflowConfig workflowConfig) {
        // sort list to guarantee step order
        List<WorkflowStep> steps = getSortedWorkflowSteps(workflowConfig);
        // get first step number
        return steps.get(0).getStep();
    }

    /**
     * Sort workflow steps by ascending step number
     */
    private List<WorkflowStep> getSortedWorkflowSteps(WorkflowConfig workflowConfig) {
        List<WorkflowStep> steps = workflowConfig.getSteps();
        steps.sort(Comparator.comparingInt(WorkflowStep::getStep));
        return steps;
    }

}
