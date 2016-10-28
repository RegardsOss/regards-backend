/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.signature;

import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.domain.Output;

/**
 * REST interface to define the entry points of the module.
 *
 */
public interface IJobInfoSignature {

    /**
     * Define the endpoint to retrieve the list of JobInfo
     *
     * @return A {@link List} of jobInfo as {@link JobInfo} wrapped in an {@link HttpEntity}
     */
    @GetMapping("/jobs")
    ResponseEntity<List<Resource<JobInfo>>> retrieveJobs();

    /**
     * Define the endpoint to retrieve the list of JobInfo depending of their state
     *
     * @param state
     *            filter by that state
     * @return job list
     */
    @GetMapping("/jobs/state/{state}")
    ResponseEntity<List<Resource<JobInfo>>> retrieveJobsByState(JobStatus state);

    /**
     * Define the endpoint to retrieve an JobInfo
     *
     * @param pJobInfoId
     *            The jobInfo id
     * @return the corresponding jobInfo
     */
    @GetMapping("/jobs/{job_id}")
    ResponseEntity<Resource<JobInfo>> retrieveJobInfo(Long pJobInfoId);

    /**
     * Define the endpoint to stop a job
     *
     * @param pJobInfoId
     *            The jobInfo id
     * @return jobInfo
     */
    @DeleteMapping("/jobs/{job_id}")
    ResponseEntity<Resource<JobInfo>> stopJob(Long pJobInfoId);

    /**
     * Define the endpoint to retrieve job results
     *
     * @param pId
     *            The jobInfo id
     * @return the list of result for that JobInfo
     */
    @GetMapping("/jobs/{job_id}/results}")
    ResponseEntity<List<Output>> getJobResults(Long pJobInfoId);

}
