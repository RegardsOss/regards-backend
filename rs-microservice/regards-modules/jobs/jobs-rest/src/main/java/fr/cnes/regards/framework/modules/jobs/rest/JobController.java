/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.modules.jobs.rest;

import java.util.List;
import java.util.UUID;

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
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobResult;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * @author Léo Mieulet
 * @author Christophe Mertz
 */
@RestController
@ModuleInfo(name = "jobs", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(JobController.JOBS)
public class JobController implements IResourceController<JobInfo> {

    /**
     * REST mapping resource : /jobs
     */
    public static final String JOBS = "/jobs";

    /**
     * Job info service
     */
    @Autowired
    private IJobInfoService jobInfoService;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Constructor
     */
    public JobController() {
        super();
    }

    /**
     * Define the endpoint to retrieve the list of JobInfo
     * @return A {@link List} of jobInfo as {@link JobInfo} wrapped in an {@link HttpEntity}
     */
    @ResourceAccess(description = "Retrieve all the jobs", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Resource<JobInfo>>> retrieveJobs() {
        return ResponseEntity.ok(toResources(jobInfoService.retrieveJobs()));
    }

    /**
     * Define the endpoint to retrieve the list of JobInfo depending of their state
     * @param state filter by that state
     * @return job list
     */
    @ResourceAccess(description = "Retrieve all the jobs with a specific state", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(value = "/state/{state}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Resource<JobInfo>>> retrieveJobsByState(@PathVariable("state") final JobStatus state) {
        return ResponseEntity.ok(toResources(jobInfoService.retrieveJobs(state)));
    }

    /**
     * Define the endpoint to retrieve a JobInfo
     * @param jobInfoId The {@link JobInfo} id
     * @return the corresponding jobInfo
     * @throws EntityNotFoundException The job does not exist
     */
    @ResourceAccess(description = "Retrieve a job", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(value = "/{jobId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource<JobInfo>> retrieveJobInfo(@PathVariable("jobId") final UUID jobInfoId)
            throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(jobInfoService.retrieveJob(jobInfoId)));
    }

    /**
     * Define the endpoint to stop a job
     * @param jobInfoId The {@link JobInfo} id
     * @return jobInfo
     * @throws EntityNotFoundException The job does not exist
     */
    @ResourceAccess(description = "Stop a job", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(value = "/{jobId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> stopJob(@PathVariable("jobId") final UUID jobInfoId)
            throws EntityNotFoundException {
        jobInfoService.stopJob(jobInfoId);
        return ResponseEntity.ok(null);
    }

    /**
     * Define the endpoint to retrieve job results
     * @param jobInfoId The {@link JobInfo} id
     * @return the list of result for that JobInfo
     * @throws EntityNotFoundException The job does not exist
     */
    @ResourceAccess(description = "Retrieve the job's results", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(value = "/{jobId}/results", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Resource<JobResult>>> getJobResults(@PathVariable("jobId") final UUID jobInfoId)
            throws EntityNotFoundException {
        final JobInfo jobInfo = jobInfoService.retrieveJob(jobInfoId);
        List<Resource<JobResult>> resources = null;
//        if (jobInfo.getResults() != null) {
//            resources = jobInfo.getResults().stream().map(u -> new Resource<>(u)).collect(Collectors.toList());
//        }
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public Resource<JobInfo> toResource(JobInfo element, Object... extras) {
        Resource<JobInfo> resource = null;
        if ((element != null) && (element.getId() != null)) {
            resource = resourceService.toResource(element);
            resourceService.addLink(resource, this.getClass(), "retrieveJobInfo", LinkRels.SELF,
                                    MethodParamFactory.build(UUID.class, element.getId()));
            resourceService.addLink(resource, this.getClass(), "retrieveJobs", LinkRels.LIST);
            resourceService.addLink(resource, this.getClass(), "stopJob", "stop",
                                    MethodParamFactory.build(UUID.class, element.getId()));
            resourceService.addLink(resource, this.getClass(), "getJobResults", "results",
                                    MethodParamFactory.build(UUID.class, element.getId()));
        }
        return resource;
    }

}
