/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy;

import java.util.List;

/**
 *
 */
public class JobAllocationStrategyResponse {

    private final String projectName;

    private final List<IJobQueue> jobQueueList;

    /**
     * @param pProjectNameToPull
     * @param pJobQueueList
     */
    public JobAllocationStrategyResponse(String pProjectNameToPull, List<IJobQueue> pJobQueueList) {
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
