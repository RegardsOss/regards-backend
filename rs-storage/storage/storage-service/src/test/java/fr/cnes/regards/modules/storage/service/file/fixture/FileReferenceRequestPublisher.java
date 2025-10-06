/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */

package fr.cnes.regards.modules.storage.service.file.fixture;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.storage.service.file.handler.FilesReferenceRequestEventHandler;
import fr.cnes.regards.modules.storage.service.file.request.FileReferenceRequestJobSchedulingService;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Navarro
 **/
@Service
@Slf4j
public class FileReferenceRequestPublisher {

    @Autowired
    protected FileReferenceRequestJobSchedulingService referenceRequestSchedulingService;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    FilesReferenceRequestEventHandler referenceRequestEventHandler;

    @Autowired
    IJobService jobService;

    @Value("PROJECT")
    private String defaultTenant;

    protected String getDefaultTenant() {
        return defaultTenant;
    }

    public void publishReferenceEvents(int expectedJobCount, FilesReferenceEvent... events) {
        publishReferenceEvents(expectedJobCount, List.of(events));
    }

    public void publishReferenceEvents(int expectedJobCount, List<FilesReferenceEvent> events) {
        referenceRequestEventHandler.handleBatch(events);
        Awaitility.await().pollDelay(5L, TimeUnit.SECONDS).until(() -> true);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        scheduleAndRunJob(expectedJobCount);
    }

    private Collection<JobInfo> scheduleAndRunJob(int expectedJobCount) {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Schedule job to initiate the FileReferenceRequestJob associated to the FileReferenceRequestAggregation
        // created earlier. Run the job till completion.
        final Collection<JobInfo> jobs = referenceRequestSchedulingService.scheduleJobs(FileRequestStatus.TO_DO);
        if (expectedJobCount >= 0) {
            assertThat(jobs).as("Reference job should be scheduled").hasSize(expectedJobCount);
        }
        // Run Job and wait for the end
        runAndWaitJob(jobs);
        return jobs;
    }

    public void runAndWaitJob(Collection<JobInfo> jobs) {
        // Run Job and wait for end
        String tenant = runtimeTenantResolver.getTenant();
        try {
            Iterator<JobInfo> it = jobs.iterator();
            List<RunnableFuture<Void>> list = Lists.newArrayList();
            while (it.hasNext()) {
                list.add(jobService.runJob(it.next(), tenant));
            }
            for (RunnableFuture<Void> futur : list) {
                log.info("Waiting synchronous job ...");
                futur.get(120L, TimeUnit.SECONDS);
                log.info("Synchronous job ends");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        } finally {
            runtimeTenantResolver.forceTenant(tenant);
        }
    }
}
