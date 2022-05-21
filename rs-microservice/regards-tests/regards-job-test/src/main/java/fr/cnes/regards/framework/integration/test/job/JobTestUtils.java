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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
     * @param jobs           jobs to execute till the end
     * @param maxJobDuration duration in second to wait before job timeout
     * @return jobInfos updated
     */
    public List<JobInfo> runAndWaitJob(Collection<JobInfo> jobs, long maxJobDuration) {
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        try {
            Iterator<JobInfo> it = jobs.iterator();
            List<RunnableFuture<Void>> list = com.google.common.collect.Lists.newArrayList();
            while (it.hasNext()) {
                JobInfo next = it.next();
                next.getParameters();
                list.add(jobService.runJob(next, tenant));
            }
            for (RunnableFuture<Void> futur : list) {
                logger.info("Waiting synchronous job ...");
                futur.get(maxJobDuration, TimeUnit.SECONDS);
                logger.info("Synchronous job ends");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Job took too much time to complete - it should not last more than {}s - {}",
                         maxJobDuration,
                         e);
            Assert.fail(e.getMessage());
        } finally {
            runtimeTenantResolver.forceTenant(tenant);
        }
        // Retrieve the updated list of job infos, as their status updated
        List<JobInfo> jobsInfoUpdated = Lists.newArrayList();
        for (JobInfo jobInfo : jobs) {
            // All jobs were returned, but their status are not updated yet
            Awaitility.await("waiting the job done").pollInterval(Duration.ofMillis(10)).until(() -> {
                runtimeTenantResolver.forceTenant(tenant);
                JobInfo jobInfoUpd = jobInfoService.retrieveJob(jobInfo.getId());
                return jobInfoUpd.getStatus().getStatus() == JobStatus.SUCCEEDED
                    || jobInfoUpd.getStatus().getStatus() == JobStatus.FAILED;
            });
            jobsInfoUpdated.add(jobInfoService.retrieveJob(jobInfo.getId()));
        }
        // Get updated jobInfo
        return jobsInfoUpdated;
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
