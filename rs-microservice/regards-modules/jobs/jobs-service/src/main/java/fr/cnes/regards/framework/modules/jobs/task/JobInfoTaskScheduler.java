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
package fr.cnes.regards.framework.modules.jobs.task;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
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

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * @author Iliana Ghazali
 **/
@Component
@Profile("!noscheduler")
@EnableScheduling
public class JobInfoTaskScheduler extends AbstractTaskScheduler {

    public static final String UPDATE_PENDING_TRIGGER_JOBS_TITLE = "Update pending jobs to be triggered scheduling";

    public static final Long MAX_TASK_DELAY = 120L; // In second

    private static final Logger LOGGER = LoggerFactory.getLogger(JobInfoTaskScheduler.class);

    private static final String UPDATE_PENDING_TRIGGER_JOBS = "UPDATE PENDING JOBS TO BE TRIGGERED";

    private static final String UPDATE_PENDING_TRIGGER_JOBS_LOCK = "updatePendingJobsToBeTriggered";

    private static final String CLEAN_OUT_OF_DATE_JOBS = "CLEAN OUT OF DATE JOBS";

    private static final String CLEAN_OUT_OF_DATE_JOBS_TITLE = "Cleaning out of date jobs";

    private static final String CLEAN_OUT_OF_DATE_JOBS_LOCK = "cleanOutOfDateJobsLock";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private JobInfoService jobInfoService;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    @Value("${regards.jobs.pool.size:10}")
    private int poolSize;

    /**
     * Task to trigger pending jobs
     */
    private final LockingTaskExecutor.Task triggerPendingJobTask = () -> {
        lockingTaskExecutors.assertLocked();
        // start searching pending jobs to be triggered
        OffsetDateTime currentDateTime = OffsetDateTime.now();
        LOGGER.debug("[{}] Search jobs with pending status and trigger date before {}",
                     INSTANCE_RANDOM_ID,
                     currentDateTime);
        jobInfoService.updatePendingJobsToBeTriggered(currentDateTime, poolSize);
    };

    private final LockingTaskExecutor.Task cleanDeletableJobsTask = () -> {
        lockingTaskExecutors.assertLocked();
        LOGGER.debug("[{}] Running clean deletable jobs task", INSTANCE_RANDOM_ID);
        jobInfoService.cleanOutOfDateJobsOnTenant();
    };

    /**
     * Periodically check if pending jobs have to be triggered (only if a trigger timestamp is present).
     * Change the job status to QUEUED if the trigger date is expired in order to launch the job execution.
     */
    @Scheduled(fixedDelayString = "${regards.jobs.trigger.update.rate.ms:60000}")
    public void triggerPendingJobs() {
        runScheduledTask(triggerPendingJobTask,
                         UPDATE_PENDING_TRIGGER_JOBS_LOCK,
                         UPDATE_PENDING_TRIGGER_JOBS,
                         UPDATE_PENDING_TRIGGER_JOBS_TITLE);
    }

    @Scheduled(fixedDelayString = "${regards.jobs.out.of.date.cleaning.rate.ms:3600000}", initialDelay = 30_000)
    public void cleanOutOfDateJobs() {
        runScheduledTask(cleanDeletableJobsTask,
                         CLEAN_OUT_OF_DATE_JOBS_LOCK,
                         CLEAN_OUT_OF_DATE_JOBS,
                         CLEAN_OUT_OF_DATE_JOBS_TITLE);
    }

    private void runScheduledTask(LockingTaskExecutor.Task taskToRun,
                                  String lockName,
                                  String taskName,
                                  String taskTitle) {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            try {
                runtimeTenantResolver.forceTenant(tenant);
                lockingTaskExecutors.executeWithLock(taskToRun,
                                                     new LockConfiguration(Instant.now(),
                                                                           lockName,
                                                                           Duration.ofSeconds(MAX_TASK_DELAY),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(taskName, taskTitle, e);
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