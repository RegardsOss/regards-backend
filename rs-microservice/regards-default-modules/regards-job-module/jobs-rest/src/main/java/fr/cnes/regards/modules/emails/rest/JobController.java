/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.jobs.domain.Output;
import fr.cnes.regards.modules.jobs.service.service.IJobInfoService;
import fr.cnes.regards.modules.jobs.service.service.JobInfoService;
import fr.cnes.regards.modules.jobs.signature.IJobInfoSignature;

/**
 * @author Léo Mieulet
 * @author Christophe Mertz
 */
@RestController
@ModuleInfo(name = "jobs", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class JobController implements IJobInfoSignature {

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

    @Override
    public ResponseEntity<List<Resource<JobInfo>>> retrieveJobs() {
        final List<JobInfo> jobInfoList = jobInfoService.retrieveJobInfoList();
        final List<Resource<JobInfo>> resources = jobInfoList.stream().map(u -> new Resource<>(u))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);

    }

    @Override
    public ResponseEntity<List<Resource<JobInfo>>> retrieveJobsByState(final JobStatus pState) {
        final List<JobInfo> jobInfoList = jobInfoService.retrieveJobInfoListByState(pState);
        final List<Resource<JobInfo>> resources = jobInfoList.stream().map(u -> new Resource<>(u))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<JobInfo>> retrieveJobInfo(final Long pJobInfoId) {
        final JobInfo jobInfo = jobInfoService.retrieveJobInfoById(pJobInfoId);
        final Resource<JobInfo> resource = new Resource<>(jobInfo);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Resource<JobInfo>> stopJob(final Long pJobInfoId) {
        final JobInfo jobInfo = jobInfoService.stopJob(pJobInfoId);
        final Resource<JobInfo> resource = new Resource<>(jobInfo);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<Output>> getJobResults(final Long pJobInfoId) {
        return null;
    }

}
