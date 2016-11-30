/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
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
public class JobController implements IResourceController<JobInfo> {

    /**
     * Business service for {@link JobInfo}.
     */
    private final IJobInfoService jobInfoService;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

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
    @ResourceAccess(description = "Retrieve all the jobs", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Resource<JobInfo>>> retrieveJobs() {
        return ResponseEntity.ok(toResources(jobInfoService.retrieveJobInfoList()));

    }

    /**
     * Define the endpoint to retrieve the list of JobInfo depending of their state
     *
     * @param pState
     *            filter by that state
     * @return job list
     */
    @ResourceAccess(description = "Retrieve all the jobs with a specific state", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(value = "/state/{state}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Resource<JobInfo>>> retrieveJobsByState(@PathVariable("state") final JobStatus pState) {
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
     * @throws EntityNotFoundException
     *             The job does not exist
     */
    @ResourceAccess(description = "Retrieve a job", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(value = "/{jobId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource<JobInfo>> retrieveJobInfo(@PathVariable("jobId") final Long pJobInfoId)
            throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(jobInfoService.retrieveJobInfoById(pJobInfoId)));
    }

    /**
     * Define the endpoint to stop a job
     *
     * @param pJobInfoId
     *            The {@link JobInfo} id
     * @return jobInfo
     * @throws EntityNotFoundException
     *             The job does not exist
     */
    @ResourceAccess(description = "Stop a job", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(value = "/{jobId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource<JobInfo>> stopJob(@PathVariable("jobId") final Long pJobInfoId)
            throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(jobInfoService.stopJob(pJobInfoId)));
    }

    /**
     * Define the endpoint to retrieve job results
     *
     * @param pJobInfoId
     *            The {@link JobInfo} id
     * @return the list of result for that JobInfo
     * @throws EntityNotFoundException
     *             The job does not exist
     */
    @ResourceAccess(description = "Retrieve the job's results", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(value = "/{jobId}/results", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Resource<Output>>> getJobResults(@PathVariable("jobId") final Long pJobInfoId)
            throws EntityNotFoundException {
        final JobInfo jobInfo = jobInfoService.retrieveJobInfoById(pJobInfoId);
        List<Resource<Output>> resources = null;
        if (jobInfo.getResult() != null) {
            resources = jobInfo.getResult().stream().map(u -> new Resource<>(u)).collect(Collectors.toList());
        }
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public Resource<JobInfo> toResource(JobInfo pElement, Object... pExtras) {
        Resource<JobInfo> resource = null;
        if (pElement != null && pElement.getId() != null) {
            resource = resourceService.toResource(pElement);
            resourceService.addLink(resource, this.getClass(), "retrieveJobInfo", LinkRels.SELF,
                                    MethodParamFactory.build(Long.class, pElement.getId()));
            resourceService.addLink(resource, this.getClass(), "retrieveJobs", LinkRels.LIST);
            resourceService.addLink(resource, this.getClass(), "stopJob", "stop",
                                    MethodParamFactory.build(Long.class, pElement.getId()));
            resourceService.addLink(resource, this.getClass(), "getJobResults", "results",
                                    MethodParamFactory.build(Long.class, pElement.getId()));
        }
        return resource;
    }

}
