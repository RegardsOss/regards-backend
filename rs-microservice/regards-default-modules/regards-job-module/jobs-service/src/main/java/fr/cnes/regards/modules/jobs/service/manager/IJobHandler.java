/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.manager;

import fr.cnes.regards.modules.jobs.domain.IEvent;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.StatusInfo;

/**
 * Provide a job pool
 */
public interface IJobHandler {

    /**
     * Store the JobInfo into the database
     *
     *
     * @param pJobInfo
     *            Store the new jobInfo in the database
     * @return the status of the new jobInfo
     */
    StatusInfo create(JobInfo pJobInfo);

    /**
     * Stop the thread pool in the next hours
     *
     */
    void shutdown();

    /**
     * Stop the thread pool in few seconds
     *
     */
    void shutdownNow();

    /**
     * Delete a job: Ensure that the job will be interrupted if it was running and change its status to Aborted
     *
     * @param pJobInfo
     *            abort the corresponding pJobInfo
     * @return the updated status of that job
     */
    StatusInfo abort(JobInfo pJobInfo);

    /**
     * Retrieve the jobInfo, then execute that job.
     *
     * @param pTenantName
     *            the project that the job belong
     * @param pJobInfoId
     *            the jobInfo id we are running
     * @return the updated status of the jobInfo status
     */
    StatusInfo execute(String pTenantName, Long pJobInfoId);

    /**
     * Receive event from jobs (throw JobMonitor)
     *
     * @param pEvent
     *            running jobs send events to JobHandler
     */
    void onEvent(IEvent pEvent);

}
