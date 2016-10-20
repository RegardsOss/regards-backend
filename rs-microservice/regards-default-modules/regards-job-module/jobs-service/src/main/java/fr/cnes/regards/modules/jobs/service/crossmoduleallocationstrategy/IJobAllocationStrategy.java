/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy;

import java.util.List;

import fr.cnes.regards.modules.project.domain.Project;

public interface IJobAllocationStrategy {

    /**
     * @param pProjects
     * @param pPreviousQueueList
     * @param pMaxThread
     * @return see JobAllocationStrategyResponse
     */
    JobAllocationStrategyResponse getNextQueue(List<Project> pProjects, List<IJobQueue> pPreviousQueueList,
            int pMaxThread);
}
