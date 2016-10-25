/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy;

import java.util.List;

/**
 * Response from JobAllocationStrategy plugin
 */
public class JobAllocationStrategyResponse {

    /**
     * The tenant name that there is a remaining slot, or null
     */
    private final String projectName;

    /**
     * the list of jobQueue
     */
    private final List<IJobQueue> jobQueueList;

    /**
     * @param pProjectNameToPull
     *            the tenant with a remaining slot
     * @param pJobQueueList
     *            the new queue list
     */
    public JobAllocationStrategyResponse(final String pProjectNameToPull, final List<IJobQueue> pJobQueueList) {
        projectName = pProjectNameToPull;
        jobQueueList = pJobQueueList;
    }

    /**
     * @return the projectName that the JobPuller will use to retrieve a job to execute
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @return the jobQueueList
     */
    public List<IJobQueue> getJobQueueList() {
        return jobQueueList;
    }
}
