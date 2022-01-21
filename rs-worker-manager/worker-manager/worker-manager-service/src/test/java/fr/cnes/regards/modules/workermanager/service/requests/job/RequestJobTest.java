/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.service.requests.job;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.workermanager.domain.request.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.service.requests.scan.AbstractRequestJobTest;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=request_job_test" })
public class RequestJobTest extends AbstractRequestJobTest {

    @Test
    public void testScanJobAndDeleteJob() {
        int nbRequests = 500;
        createRequests(nbRequests);
        SearchRequestParameters filters = new SearchRequestParameters();
        requestService.scheduleRequestDeletionJob(filters);

        // Retrieve ScanRequestJob jobs
        List<JobInfo> scanJobInfos = this.getJobTestUtils().retrieveFullJobInfos(ScanRequestJob.class);
        Assert.assertEquals("should launch correct number of ScanRequestJob", 1, scanJobInfos.size());
        // Run job
        scanJobInfos = this.getJobTestUtils().runAndWaitJob(scanJobInfos, 5);
        scanJobInfos.stream().forEach(jobInfo -> {
            Assert.assertEquals("All jobs should be ok", JobStatus.SUCCEEDED, jobInfo.getStatus().getStatus());
        });


        // Retrieve DeleteRequestJob jobs
        List<JobInfo> deleteJobInfos = this.getJobTestUtils().retrieveFullJobInfos(DeleteRequestJob.class);
        Assert.assertEquals("should launch correct number of DeleteRequestJob", 2, deleteJobInfos.size());
        // Run job
        deleteJobInfos = this.getJobTestUtils().runAndWaitJob(deleteJobInfos, 15);
        deleteJobInfos.stream().forEach(jobInfo -> {
            Assert.assertEquals("All jobs should be ok", JobStatus.SUCCEEDED, jobInfo.getStatus().getStatus());
        });

        // Check
        Page<Request> allRequests = requestService.searchRequests(new SearchRequestParameters(),
                                                                  PageRequest.of(0, 100));
        Assert.assertEquals("expect no more requests", 0, allRequests.getTotalElements());
    }

    @Ignore
    @Test
    public void testPerfDeleteJobs() {
        int nbRequests = 50000;
        createRequests(nbRequests);
        requestService.scheduleRequestDeletionJob(new SearchRequestParameters());

        // Retrieve ScanRequestJob jobs
        List<JobInfo> scanJobInfos = this.getJobTestUtils().retrieveFullJobInfos(ScanRequestJob.class);
        Assert.assertEquals("should launch correct number of ScanRequestJob", 1, scanJobInfos.size());
        // Run job
        scanJobInfos = this.getJobTestUtils().runAndWaitJob(scanJobInfos, 50);
        scanJobInfos.stream().forEach(jobInfo -> {
            Assert.assertEquals("All jobs should be ok", JobStatus.SUCCEEDED, jobInfo.getStatus().getStatus());
        });


        // Retrieve DeleteRequestJob jobs
        List<JobInfo> deleteJobInfos = this.getJobTestUtils().retrieveFullJobInfos(DeleteRequestJob.class);
        Assert.assertEquals("should launch correct number of DeleteRequestJob", 125, deleteJobInfos.size());
        // Run job
        deleteJobInfos = this.getJobTestUtils().runAndWaitJob(deleteJobInfos, 50);
        deleteJobInfos.stream().forEach(jobInfo -> {
            Assert.assertEquals("All jobs should be ok", JobStatus.SUCCEEDED, jobInfo.getStatus().getStatus());
        });

        // Check
        Page<Request> allRequests = requestService.searchRequests(new SearchRequestParameters(),
                                                                  PageRequest.of(0, 100));
        Assert.assertEquals("expect no more requests", 0, allRequests.getTotalElements());
    }
}
