/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.manager;

import fr.cnes.regards.modules.jobs.domain.IEvent;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.StatusInfo;

public interface IJobHandler {

    /**
     * Store the new job inside the database
     *
     * @param job
     * @return
     */
    StatusInfo create(JobInfo job);

    JobInfo getJob(Long jobId);

    /**
     * @return
     */
    StatusInfo shutdown();

    /**
     * @param pJob
     * @return
     */
    StatusInfo abort(JobInfo pJob);

    /**
     * @param pTenantName
     * @param pJobInfoId
     * @return
     */
    StatusInfo execute(String pTenantName, Long pJobInfoId);

    /**
     * @return
     */
    StatusInfo shutdownNow();

    /**
     * @param pEvent
     */
    void onEvent(IEvent pEvent);

}
