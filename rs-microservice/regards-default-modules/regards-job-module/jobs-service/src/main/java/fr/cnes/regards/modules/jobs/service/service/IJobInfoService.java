/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.service;

import java.util.List;

import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;

/**
 *
 */
public interface IJobInfoService {

    JobInfo createJobInfo(final JobInfo pJobInfo);

    List<JobInfo> retrieveJobInfoList();

    /**
     * @param pState
     * @return
     */
    List<JobInfo> retrieveJobInfoListByState(JobStatus pState);

    /**
     * @param pJobInfoId
     * @return
     */
    JobInfo retrieveJobInfoById(Long pJobInfoId);

    /**
     * @param pJobInfo
     * @return
     */
    JobInfo save(JobInfo pJobInfo);
}
