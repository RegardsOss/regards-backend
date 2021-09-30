/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jobs.task;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import java.time.Instant;
import java.time.OffsetDateTime;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Iliana Ghazali
 **/
@Component
@Profile("!noscheduler")
@EnableScheduling
public class JobInfoTaskScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobInfoTaskScheduler.class);

    private static final String UPDATE_PENDING_TRIGGER_JOBS = "UPDATE PENDING JOBS TO BE TRIGGERED";

    public static final String UPDATE_PENDING_TRIGGER_JOBS_TITLE = "Update pending jobs to be triggered scheduling";

    private static final String UPDATE_PENDING_TRIGGER_JOBS_LOCK = "updatePendingJobsToBeTriggered";

    public static final Long MAX_TASK_DELAY = 5L; // In second

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private JobInfoService jobInfoService;

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    @Value("${regards.jobs.pool.size:10}")
    private int poolSize;

    /**
     * Task to trigger pending jobs
     */
    private final LockingTaskExecutor.Task triggerPendingJobTask = () -> {
        LockAssert.assertLocked();
        // start searching pending jobs to be triggered
        OffsetDateTime currentDateTime = OffsetDateTime.now();
        LOGGER.debug("[{}] Search jobs with pending status and trigger date before {}", INSTANCE_RANDOM_ID,
                     currentDateTime);
        jobInfoService.updatePendingJobsToBeTriggered(currentDateTime, poolSize);
    };

    /**
     * Periodically check if pending jobs have to be triggered (only if a trigger timestamp is present).
     * Change the job status to QUEUED if the trigger date is expired in order to launch the job execution.
     */
    @Scheduled(fixedDelayString = "${regards.jobs.trigger.update.rate.ms:60000}")
    public void triggerPendingJobs() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                runtimeTenantResolver.forceTenant(tenant);
                lockingTaskExecutors.executeWithLock(triggerPendingJobTask,
                                                     new LockConfiguration(UPDATE_PENDING_TRIGGER_JOBS_LOCK,
                                                                           Instant.now().plusSeconds(MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(UPDATE_PENDING_TRIGGER_JOBS, UPDATE_PENDING_TRIGGER_JOBS_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}