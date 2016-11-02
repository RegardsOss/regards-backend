/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.service;

import java.util.List;

import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;

/**
 * @author lmieulet
 */
public interface IJobInfoService {

    /**
     * Store the JobInfo into the database, and publish it on the broker message
     *
     *
     * @param pJobInfo
     *            Store the new jobInfo in the database
     * @return the status of the new jobInfo
     */
    JobInfo createJobInfo(JobInfo pJobInfo);

    /**
     * @return returns all jobs
     */
    List<JobInfo> retrieveJobInfoList();

    /**
     * @param pState
     *            the state filter
     * @return the list of jobs matching that the provided state
     */
    List<JobInfo> retrieveJobInfoListByState(JobStatus pState);

    /**
     * @param pJobInfoId
     *            the jobInfo id
     * @return the corresponding jobInfo
     */
    JobInfo retrieveJobInfoById(Long pJobInfoId);

    /**
     * @param pJobInfo
     *            the jobInfo to save
     * @return the updated jobInfo
     */
    JobInfo save(JobInfo pJobInfo);

    /**
     * @param pJobInfoId
     *            the jobInfo id
     * @return the updated jobInfo
     */
    JobInfo stopJob(Long pJobInfoId);
}
