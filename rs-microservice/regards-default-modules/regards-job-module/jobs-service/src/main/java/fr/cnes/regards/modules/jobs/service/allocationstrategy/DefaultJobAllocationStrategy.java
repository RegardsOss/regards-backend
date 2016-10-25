/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.allocationstrategy;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobAllocationStrategy;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.IJobQueue;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.JobAllocationStrategyResponse;
import fr.cnes.regards.modules.jobs.service.crossmoduleallocationstrategy.JobQueue;

/**
 *
 */
public class DefaultJobAllocationStrategy implements IJobAllocationStrategy {

    /**
     * Go over all project id
     */
    private int currentProjectQueue = 0;

    /**
     * @return the queue name
     */
    @Override
    public JobAllocationStrategyResponse getNextQueue(final List<String> pProjects,
            final List<IJobQueue> pPreviousQueueList, final int maxThread) {
        List<IJobQueue> nextQueueList;
        // Recreate the queueList on initialization / on projectList change
        if ((pPreviousQueueList == null) || (pProjects.size() != pPreviousQueueList.size())) {
            nextQueueList = initializeQueueList(pProjects, pPreviousQueueList, maxThread);
        } else {
            nextQueueList = pPreviousQueueList;
        }
        String resultingProjectName = null;

        // try to found a project that can accept a new job
        for (int i = 0; i < pProjects.size(); i++) {
            final String project = pProjects.get(currentProjectQueue);
            for (int j = 0; j < nextQueueList.size(); j++) {
                // Execute a job for that project only if there is still a place
                if (nextQueueList.get(j).getName().equals(project)
                        && (nextQueueList.get(j).getCurrentSize() < nextQueueList.get(j).getMaxSize())) {
                    resultingProjectName = project;
                    break;
                }
            }
            currentProjectQueue = (currentProjectQueue + 1) % pProjects.size();

            if (resultingProjectName != null) {
                break;
            }
        }

        return new JobAllocationStrategyResponse(resultingProjectName, nextQueueList);
    }

    /**
     *
     * @param pProjects
     *            the project list
     * @param pPreviousQueueList
     *            the previous queue list
     * @param pMaxThread
     *            the number of thread slot for the current microservice
     * @return the new job queue
     */
    protected List<IJobQueue> initializeQueueList(final List<String> pProjects,
            final List<IJobQueue> pPreviousQueueList, final int pMaxThread) {
        final List<IJobQueue> queueList = new ArrayList<>();
        final int nbJobsPerProject = ((Double) Math.ceil((pMaxThread) / ((double) pProjects.size()))).intValue();
        for (int i = 0; i < pProjects.size(); i++) {
            int projectNbJobs = 0;
            // If the previous queue list contained the project, then reuses the number of current jobs for that queue
            final String projectName = pProjects.get(i);
            if (pPreviousQueueList != null) {

                for (int j = 0; j < pPreviousQueueList.size(); j++) {
                    if (pPreviousQueueList.get(j).getName().equals(projectName)) {
                        projectNbJobs = pPreviousQueueList.get(j).getCurrentSize();
                        break;
                    }
                }
            }
            queueList.add(new JobQueue(projectName, projectNbJobs, nbJobsPerProject));
        }
        return queueList;

    }
}
