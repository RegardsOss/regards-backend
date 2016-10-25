/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.domain.Output;
import fr.cnes.regards.modules.jobs.service.service.IJobInfoService;
import fr.cnes.regards.modules.jobs.signature.IJobInfoSignature;

/**
 *
 */
@RestController
@ModuleInfo(name = "jobs", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class JobController implements IJobInfoSignature {

    /**
     * JobInfo Service
     */
    private final IJobInfoService jobInfoService;

    public JobController(final IJobInfoService pJobInfoService) {
        jobInfoService = pJobInfoService;
    }

    @Override
    public HttpEntity<List<Resource<JobInfo>>> retrieveJobs() {
        final List<JobInfo> jobInfoList = jobInfoService.retrieveJobInfoList();
        final List<Resource<JobInfo>> resources = jobInfoList.stream().map(u -> new Resource<>(u))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);

    }

    @Override
    public HttpEntity<List<Resource<JobInfo>>> retrieveJobsByState(final JobStatus pState) {
        final List<JobInfo> jobInfoList = jobInfoService.retrieveJobInfoListByState(pState);
        final List<Resource<JobInfo>> resources = jobInfoList.stream().map(u -> new Resource<>(u))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public HttpEntity<Resource<JobInfo>> retrieveJobInfo(final Long pJobInfoId) {
        final JobInfo jobInfoList = jobInfoService.retrieveJobInfoById(pJobInfoId);
        final Resource<JobInfo> resource = new Resource<>(jobInfoList);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public HttpEntity<Resource<JobInfo>> stopJob(final Long pJobInfoId) {
        return null;
    }

    @Override
    public HttpEntity<List<Output>> getJobResults(final Long pJobInfoId) {
        return null;
    }

}
