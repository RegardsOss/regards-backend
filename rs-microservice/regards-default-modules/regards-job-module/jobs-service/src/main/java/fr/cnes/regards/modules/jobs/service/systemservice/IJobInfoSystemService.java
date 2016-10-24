/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.systemservice;

import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;

/**
 *
 */
public interface IJobInfoSystemService {

    /**
     * @param tenantName
     * @param jobInfoId
     */
    JobInfo findJobInfo(final String tenantName, final Long jobInfoId);

    /**
     * @param pTenantId
     * @param pJobInfo
     * @return
     */
    JobInfo updateJobInfo(String pTenantId, JobInfo pJobInfo);

    /**
     * Setup the end date
     *
     * @param pJobInfoId
     *            the jobInfo id
     * @param pJobStatus
     *            the new jobStatus
     */
    JobInfo updateJobInfoToDone(final Long pJobInfoId, final JobStatus pJobStatus, final String tenantName);
}
