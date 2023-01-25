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
import fr.cnes.regards.modules.workermanager.domain.config.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service to handle {@link fr.cnes.regards.modules.workermanager.domain.config.Workflow}
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
     * Find specific workerType on a workflow with index. Null if index is greater than workflow size.
     *
     * @param workflow containing workerType to find
     * @param step     workerType position to retrieve
     * @return workerType. Can be null if none was found.
     */
    public String getWorkerTypeInWorkflow(Workflow workflow, int step) {
        List<String> workerTypes = workflow.getWorkerTypes();
        String workerType = null;
        if (step < workerTypes.size()) {
            workerType = workerTypes.get(step);
        }
        return workerType;
    }

    public Optional<Workflow> findWorkflowByType(String workflowType) {
        return workflowRepository.findById(workflowType);
    }

    public boolean hasNextWorkerTypeStep(Workflow workflow, int step) {
        return step + 1 < workflow.getWorkerTypes().size();
    }

}
