/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.manager;

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

    StatusInfo delete(JobInfo job);

    /**
     * @param pJobId
     * @return
     */
    StatusInfo execute(Long pJobId);

    JobInfo getJob(Long jobId);

    StatusInfo handle(JobInfo job);

    StatusInfo restart(JobInfo job);

    StatusInfo stop(Long jobId);

}
