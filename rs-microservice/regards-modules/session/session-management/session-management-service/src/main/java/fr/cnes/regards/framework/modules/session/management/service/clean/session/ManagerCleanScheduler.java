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
package fr.cnes.regards.framework.modules.session.management.service.clean.session;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.session.management.service.update.ManagerSnapshotJob;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import java.time.Instant;
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
 * Scheduler to clean old {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep},
 * {@link fr.cnes.regards.framework.modules.session.management.domain.Session} and empty
 * {@link fr.cnes.regards.framework.modules.session.management.domain.Source}
 *
 * @author Iliana Ghazali
 */
@Profile("!noscheduler")
@Component
@EnableScheduling
public class ManagerCleanScheduler extends AbstractTaskScheduler {

    public static final Logger LOGGER = LoggerFactory.getLogger(ManagerCleanScheduler.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ManagerCleanJobService managerCleanJobService;

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    @Autowired
    private IJobInfoService jobService;

    @Value("${spring.application.name}")
    private static String microserviceName;

    @Value("${regards.session.manager.clean.session.cron}")
    private static final String CRON = "0 0 0 1-7 * SUN";

    public static final Long MAX_TASK_DELAY = 60L; // In second

    public static final String CLEAN_SESSION = "Clean session, session steps and unused sources";

    public static final String CLEAN_SESSION_LOCK = microserviceName + "_clean-session";

    public static final String CLEAN_SESSION_TITLE = "Clean session scheduling";

    /**
     * Snapshot task
     */
    private final LockingTaskExecutor.Task snapshotProcessTask = () -> {
        LockAssert.assertLocked();
        managerCleanJobService.scheduleJob();
    };

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(cron = CRON)
    protected void scheduleCleanSession() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, CLEAN_SESSION);
                boolean isSnapshotJobsFinished = waitUntilManagerSnapshotJobEnd();
                if (isSnapshotJobsFinished) {
                    lockingTaskExecutors.executeWithLock(snapshotProcessTask, new LockConfiguration(CLEAN_SESSION_LOCK,
                                                                                                    Instant.now()
                                                                                                            .plusSeconds(
                                                                                                                    MAX_TASK_DELAY)));
                } else {
                    LOGGER.warn("{} could not be executed because AgentSnapshotJobs did not finished on time",
                                CLEAN_SESSION_TITLE);
                }
            } catch (Throwable e) {
                handleSchedulingError(CLEAN_SESSION, CLEAN_SESSION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    private boolean waitUntilManagerSnapshotJobEnd() {
        // wait 5 minutes until all AgentSnapshotJobs end
        final long waitDuration = 300000L;
        final long sleepDuration = 45000L;
        long count;
        long now = System.currentTimeMillis();
        long end = now + waitDuration;
        do {
            count = this.jobService
                    .retrieveJobsCount(ManagerSnapshotJob.class.getName(), JobStatus.QUEUED, JobStatus.PENDING,
                                       JobStatus.RUNNING);
            now = System.currentTimeMillis();
            // wait 45s if jobs are currently running
            if (count != 0) {
                try {
                    Thread.sleep(sleepDuration);
                } catch (InterruptedException e) {
                    LOGGER.warn("Warning - the thread was interrupted while waiting for AgentSnapshotJob ending", e);
                    // Restore interrupted state
                    Thread.currentThread().interrupt();
                }
            }
        } while (count != 0 && end < now);
        return count == 0;
    }
}