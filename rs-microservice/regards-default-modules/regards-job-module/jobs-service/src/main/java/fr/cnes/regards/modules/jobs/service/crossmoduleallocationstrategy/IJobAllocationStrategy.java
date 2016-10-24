/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy;

import java.util.List;

import fr.cnes.regards.modules.project.domain.Project;

/**
 * Interface for plugin JobAllocationStrategy
 */
@FunctionalInterface
public interface IJobAllocationStrategy {

    /**
     * @param pProjects
     *            the project list
     * @param pPreviousQueueList
     *            the previous queue list
     * @param pMaxThread
     *            the number of thread allowed on the same time
     * @return see JobAllocationStrategyResponse
     */
    JobAllocationStrategyResponse getNextQueue(List<Project> pProjects, List<IJobQueue> pPreviousQueueList,
            int pMaxThread);
}
