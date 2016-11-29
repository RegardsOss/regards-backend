/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.domain.Output;
import fr.cnes.regards.modules.jobs.service.service.IJobInfoService;
import fr.cnes.regards.modules.jobs.service.service.JobInfoService;

/**
 * @author Léo Mieulet
 * @author Christophe Mertz
 */
@RestController
@ModuleInfo(name = "jobs", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/jobs")
public class JobController {

    /**
     * Business service for {@link JobInfo}.
     */
    private final IJobInfoService jobInfoService;

    /**
     * Constructor to specify a particular {@link IJobInfoService}.
     * 
     * @param pJobInfoService
     *            The {@link JobInfoService} used
     */
    public JobController(final IJobInfoService pJobInfoService) {
        super();
        jobInfoService = pJobInfoService;
    }

    /**
     * Define the endpoint to retrieve the list of JobInfo
     *
     * @return A {@link List} of jobInfo as {@link JobInfo} wrapped in an {@link HttpEntity}
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Resource<JobInfo>>> retrieveJobs() {
        final List<JobInfo> jobInfoList = jobInfoService.retrieveJobInfoList();
        final List<Resource<JobInfo>> resources = jobInfoList.stream().map(u -> new Resource<>(u))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);

    }

    /**
     * Define the endpoint to retrieve the list of JobInfo depending of their state
     *
     * @param pState
     *            filter by that state
     * @return job list
     */
    @RequestMapping(value = "/state/{state}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Resource<JobInfo>>> retrieveJobsByState(@PathVariable("state") final JobStatus pState) {
        final List<JobInfo> jobInfoList = jobInfoService.retrieveJobInfoListByState(pState);
        final List<Resource<JobInfo>> resources = jobInfoList.stream().map(u -> new Resource<>(u))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Define the endpoint to retrieve a JobInfo
     *
     * @param pJobInfoId
     *            The {@link JobInfo} id
     * @return the corresponding jobInfo
     */
    @RequestMapping(value = "/{jobId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Resource<JobInfo>> retrieveJobInfo(@PathVariable("jobId") final Long pJobInfoId) {
        final JobInfo jobInfo = jobInfoService.retrieveJobInfoById(pJobInfoId);
        final Resource<JobInfo> resource = new Resource<>(jobInfo);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Define the endpoint to stop a job
     *
     * @param pJobInfoId
     *            The {@link JobInfo} id
     * @return jobInfo
     */
    @RequestMapping(value = "/{jobId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Resource<JobInfo>> stopJob(@PathVariable("jobId") final Long pJobInfoId) {
        final JobInfo jobInfo = jobInfoService.stopJob(pJobInfoId);
        final Resource<JobInfo> resource = new Resource<>(jobInfo);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Define the endpoint to retrieve job results
     *
     * @param pJobInfoId
     *            The {@link JobInfo} id
     * @return the list of result for that JobInfo
     */
    @RequestMapping(value = "/{jobId}/results", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<Resource<Output>>> getJobResults(@PathVariable("jobId") final Long pJobInfoId) {
        final JobInfo jobInfo = jobInfoService.retrieveJobInfoById(pJobInfoId);
        List<Resource<Output>> resources = null;
        if (jobInfo.getResult() != null) {
            resources = jobInfo.getResult().stream().map(u -> new Resource<>(u)).collect(Collectors.toList());
        }
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

}
