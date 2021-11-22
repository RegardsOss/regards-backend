/*
 * Copyright 2017-2021 'CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.service.requests.scan;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.workermanager.domain.request.SearchRequestParameters;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import fr.cnes.regards.modules.workermanager.service.JobsPriority;
import fr.cnes.regards.modules.workermanager.service.requests.job.DispatchRequestJob;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=request_scan_test" })
public class RequestScanServiceTest extends AbstractRequestJobTest {

    @Autowired
    private RequestScanService requestScanService;


    @Test
    public void testScanThenDispatch() throws Throwable {
        int nbRequests = 5;
        createRequests(nbRequests);
        // Scan
        requestScanService.scanNoWorkerAvailableRequests();

        // Retrieve Dispatch jobs
        List<JobInfo> jobInfos = this.getJobTestUtils().retrieveFullJobInfos(DispatchRequestJob.class);
        Assert.assertEquals("should launch correct number of RepublishRequestJob", 1, jobInfos.size());

        // Check job
        Optional<JobInfo> jobInfoOpt = jobInfos.stream().findFirst();
        Assert.assertTrue("should launch correct number of RepublishRequestJob", jobInfoOpt.isPresent());
        Assert.assertEquals("should get the correct priority",
                            Integer.valueOf(JobsPriority.REQUEST_DISPATCH_JOB.getPriority()),
                            jobInfoOpt.get().getPriority());

        // Run job
        jobInfos = this.getJobTestUtils().runAndWaitJob(jobInfos, 5);
        jobInfos.stream().forEach(jobInfo -> {
            Assert.assertEquals("All jobs should be ok", JobStatus.SUCCEEDED, jobInfo.getStatus().getStatus());
        });

        // Check
        Assert.assertEquals("still the expected number of requests", nbRequests, requestRepository.findAll().size());
        Page<Request> runningRequests = requestService.searchRequests(
                new SearchRequestParameters().withStatusesIncluded(RequestStatus.DISPATCHED), PageRequest.of(100, 5));
        Assert.assertEquals("expect all requests running", nbRequests, runningRequests.getTotalElements());
    }

    @Test
    @Ignore
    public void testScanPerformance() throws Throwable {
        createRequests(50000);
        requestScanService.scanNoWorkerAvailableRequests();
        List<JobInfo> jobInfos = this.getJobTestUtils().retrieveFullJobInfos(DispatchRequestJob.class);
        Assert.assertEquals("should launch correct number of RepublishRequestJob", 125, jobInfos.size());
    }

}
