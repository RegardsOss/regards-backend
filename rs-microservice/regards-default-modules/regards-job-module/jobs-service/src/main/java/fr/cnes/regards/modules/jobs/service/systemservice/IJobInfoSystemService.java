/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.systemservice;

import fr.cnes.regards.modules.jobs.domain.JobInfo;

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

}
