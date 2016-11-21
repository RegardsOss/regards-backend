/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.systemservice;

import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;

/**
 * @author Léo Mieulet
 */
public interface IJobInfoSystemService {

    /**
     * @param pTenantName
     *            the tenant name
     * @param pJobInfoId
     *            the jobInfo id
     * @return the jobInfo
     */
    JobInfo findJobInfo(final String pTenantName, final Long pJobInfoId);

    /**
     * @param pTenantId
     *            the tenant name
     * @param pJobInfo
     *            the jobInfo id
     * @return the updated jobInfo
     */
    JobInfo updateJobInfo(String pTenantId, JobInfo pJobInfo);

    /**
     * Setup the end date
     *
     * @param pJobInfoId
     *            the jobInfo id
     * @param pJobStatus
     *            the new jobStatus (succeeded, failed)
     * @param pTenantName
     *            the tenant name
     * @return the updated jobInfo
     */
    JobInfo updateJobInfoToDone(final Long pJobInfoId, final JobStatus pJobStatus, final String pTenantName);
}
