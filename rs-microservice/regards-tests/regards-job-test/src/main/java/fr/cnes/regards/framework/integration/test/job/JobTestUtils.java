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
package fr.cnes.regards.framework.integration.test.job;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.apache.commons.compress.utils.Lists;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utils to run jobs synchronously during test
 *
 * @author Léo Mieulet
 */
@Component
public class JobTestUtils {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected IJobService jobService;

    @Autowired
    private JobInfoService jobInfoService;

    /**
     * @param jobs                jobs to execute
     * @param maxJobDurationInSec job execution timeout duration (in seconds). After this time, the test will fail.
     * @return jobInfos updated with a final status (success or failure)
     */
    public List<JobInfo> runAndWaitJob(Collection<JobInfo> jobs, long maxJobDurationInSec) {
        return jobs.stream().map(jobInfo -> runAndWaitJob(jobInfo, maxJobDurationInSec)).toList();
    }

    /**
     * see {@link this#runAndWaitJob(Collection, long)}
     */
    public JobInfo runAndWaitJob(JobInfo jobInfo, long maxJobDurationInSec) {
        // Run job
        logger.info("Start execution of job with id '{}'.", jobInfo.getId());
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(jobInfo, tenant);
        // verify that job is properly updated at the end of its execution
        return checkJobInfoUpdate(jobInfo, tenant, maxJobDurationInSec);
    }

    /**
     * Check if a job is executed until a final status (either {@link JobStatus#SUCCEEDED} or {@link JobStatus#FAILED}).
     * See {@link fr.cnes.regards.framework.modules.jobs.service.JobThreadPoolExecutor} to know how job is handled in
     * case of error.
     * If another status is found after the timeout, something has gone wrong and the test will fail.
     */
    private JobInfo checkJobInfoUpdate(JobInfo jobInfo, String tenant, long maxJobDurationInSec) {
        // Job should be updated at the end with a final status either SUCCEEDED or FAILED
        // see JobThreadPoolExecutor to know how job is handled in case of error
        // If another status is found after the timeout, something has gone wrong.
        JobInfo updatedJobInfo = null;
        try {
            Awaitility.await("waiting for job completion")
                      .pollInterval(Duration.ofMillis(10))
                      .atMost(Duration.ofSeconds(maxJobDurationInSec))
                      .until(() -> {
                          runtimeTenantResolver.forceTenant(tenant);
                          JobInfo jobInfoUpd = jobInfoService.retrieveJob(jobInfo.getId());
                          return jobInfoUpd.getStatus().getStatus() == JobStatus.SUCCEEDED
                                 || jobInfoUpd.getStatus().getStatus() == JobStatus.FAILED;
                      });
            updatedJobInfo = jobInfoService.retrieveJob(jobInfo.getId());
        } catch (ConditionTimeoutException e) {
            Assert.fail(String.format("Job did not end in a final status in the expected amount of time. Cause: %s",
                                      e));
        }
        return updatedJobInfo;
    }

    public List<JobInfo> retrieveJobs() {
        return jobInfoService.retrieveJobs();
    }

    public Page<JobInfo> retrieveLightJobInfos(Class aClass, Pageable page, JobStatus... statuses) {
        if (statuses.length == 0) {
            // Use all statuses if none provided
            return jobInfoService.retrieveJobs(aClass.getName(),
                                               page,
                                               JobStatus.QUEUED,
                                               JobStatus.RUNNING,
                                               JobStatus.FAILED,
                                               JobStatus.TO_BE_RUN,
                                               JobStatus.PENDING,
                                               JobStatus.SUCCEEDED,
                                               JobStatus.ABORTED);
        }
        return jobInfoService.retrieveJobs(aClass.getName(), page);
    }

    public List<JobInfo> retrieveFullJobInfos(Class aClass, JobStatus... statuses) {
        Pageable page = PageRequest.of(0, 100);
        List<JobInfo> jobsInfoWithParams = Lists.newArrayList();
        boolean hasNext;
        do {
            Page<JobInfo> jobInfos = retrieveLightJobInfos(aClass, page, statuses);

            // Fetch the full JobInfo entity, as it miss parameters
            jobsInfoWithParams.addAll(jobInfos.stream()
                                              .map(jobInfo -> jobInfoService.retrieveJob(jobInfo.getId()))
                                              .collect(Collectors.toList()));
            hasNext = jobInfos.getNumber() < (jobInfos.getTotalPages() - 1);
            page = page.next();
        } while (hasNext);
        return jobsInfoWithParams;
    }

}
