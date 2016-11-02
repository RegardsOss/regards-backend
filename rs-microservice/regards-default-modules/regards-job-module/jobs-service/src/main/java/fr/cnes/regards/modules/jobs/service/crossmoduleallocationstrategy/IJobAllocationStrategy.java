/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy;

import java.util.List;

/**
 * Interface for plugin JobAllocationStrategy
 * 
 * @author lmieulet
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
    JobAllocationStrategyResponse getNextQueue(List<String> pProjects, List<IJobQueue> pPreviousQueueList,
            int pMaxThread);
}
