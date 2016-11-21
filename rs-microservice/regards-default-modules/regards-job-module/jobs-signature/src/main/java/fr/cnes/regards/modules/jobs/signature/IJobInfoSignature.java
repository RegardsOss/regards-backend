/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.signature;

import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.domain.Output;

/**
 * REST interface to define the entry points of the module.
 *
 */
@RequestMapping("/jobs")
public interface IJobInfoSignature {

    /**
     * Define the endpoint to retrieve the list of JobInfo
     *
     * @return A {@link List} of jobInfo as {@link JobInfo} wrapped in an {@link HttpEntity}
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Resource<JobInfo>>> retrieveJobs();

    /**
     * Define the endpoint to retrieve the list of JobInfo depending of their state
     *
     * @param state
     *            filter by that state
     * @return job list
     */
    @RequestMapping(value = "/state/{state}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Resource<JobInfo>>> retrieveJobsByState(@PathVariable("jobId") JobStatus state);

    /**
     * Define the endpoint to retrieve a JobInfo
     *
     * @param pJobInfoId
     *            The jobInfo id
     * @return the corresponding jobInfo
     */
    @RequestMapping(value = "/{jobId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Resource<JobInfo>> retrieveJobInfo(@PathVariable("jobId") Long pJobInfoId);

    /**
     * Define the endpoint to stop a job
     *
     * @param pJobInfoId
     *            The jobInfo id
     * @return jobInfo
     */
    @RequestMapping(value = "/{jobId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Resource<JobInfo>> stopJob(@PathVariable("jobId") Long pJobInfoId);

    /**
     * Define the endpoint to retrieve job results
     *
     * @param pId
     *            The jobInfo id
     * @return the list of result for that JobInfo
     */
    @RequestMapping(value = "/{jobId}/results", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Output>> getJobResults(@PathVariable("jobId") Long pJobInfoId);

}
