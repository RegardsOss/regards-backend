/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy;

import java.util.List;

public interface IJobAllocationStrategy {

    /**
     * @param pProjects
     * @param pPreviousQueueList
     * @param pMaxThread
     * @return see JobAllocationStrategyResponse
     */
    JobAllocationStrategyResponse getNextQueue(List<String> pProjects, List<IJobQueue> pPreviousQueueList,
            int pMaxThread);
}
