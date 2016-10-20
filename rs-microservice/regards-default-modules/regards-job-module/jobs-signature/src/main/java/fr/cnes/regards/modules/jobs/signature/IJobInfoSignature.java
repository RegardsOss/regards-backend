/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.signature;

import java.util.List;

import org.hibernate.validator.constraints.Email;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.GetMapping;

import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.Output;

/**
 * REST interface to define the entry points of the module.
 *
 */
public interface IJobInfoSignature {

    /**
     * Define the endpoint for retrieving the list of JobInfo
     *
     * @return A {@link List} of jobInfo as {@link JobInfo} wrapped in an {@link HttpEntity}
     */
    @GetMapping("/jobs")
    HttpEntity<List<JobInfo>> retrieveJobs();

    /**
     * Define the endpoint for retrieving the list of JobInfo depending of their state
     *
     * @return A {@link List} of emails as {@link Email} wrapped in an {@link HttpEntity}
     */
    @GetMapping("/jobs/state/{state}")
    HttpEntity<List<JobInfo>> retrieveJobsByState(String state);

    /**
     * Define the endpoint for retrieving an JobInfo
     *
     * @param pId
     *            The email id
     * @return The email as a {@link JobInfo} wrapped in an {@link HttpEntity}
     */
    @GetMapping("/emails/{job_id}")
    HttpEntity<JobInfo> retrieveEmail(Long pId);

    /**
     * Define the endpoint for retrieving
     *
     * @param pId
     *            The job id
     * @return the list of result for that JobInfo
     */
    @GetMapping("/emails/{job_id}/results}")
    HttpEntity<List<Output>> getJobResults(Long pId);

}
