package fr.cnes.regards.framework.modules.jobs.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import feign.Headers;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobResult;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;

/**
 * Feign client exposing the jobs module endpoints to other microservices plugged through Eureka.
 */
@FeignClient("#{'${spring.application.name}'}")
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface JobInfoClient {

    /**
     * Define the endpoint to retrieve the list of JobInfo
     * @return A {@link List} of jobInfo as {@link JobInfo} wrapped in an {@link HttpEntity}
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Resource<JobInfo>>> retrieveJobs();

    /**
     * Define the endpoint to retrieve the list of JobInfo depending of their state
     * @param pState filter by that state
     * @return job list
     */
    @RequestMapping(value = "/state/{state}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Resource<JobInfo>>> retrieveJobsByState(@PathVariable("state") final JobStatus pState);

    /**
     * Define the endpoint to retrieve a JobInfo
     * @param pJobInfoId The {@link JobInfo} id
     * @return the corresponding jobInfo
     */
    @RequestMapping(value = "/{jobId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Resource<JobInfo>> retrieveJobInfo(@PathVariable("jobId") final Long pJobInfoId);

    /**
     * Define the endpoint to stop a job
     * @param pJobInfoId The {@link JobInfo} id
     * @return jobInfo
     */
    @RequestMapping(value = "/{jobId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Resource<JobInfo>> stopJob(@PathVariable("jobId") final Long pJobInfoId);

    /**
     * Define the endpoint to retrieve job results
     * @param pJobInfoId The {@link JobInfo} id
     * @return the list of result for that JobInfo
     */
    @RequestMapping(value = "/{jobId}/results", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Resource<JobResult>>> getJobResults(@PathVariable("jobId") final Long pJobInfoId);

}