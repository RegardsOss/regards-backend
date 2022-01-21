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
package fr.cnes.regards.framework.modules.session.manager.service.clean.session;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.session.manager.domain.Session;
import fr.cnes.regards.framework.modules.session.manager.domain.Source;
import fr.cnes.regards.framework.modules.session.manager.service.update.ManagerSnapshotJob;
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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduler to clean old {@link fr.cnes.regards.framework.modules.session.commons.domain.SessionStep},
 * {@link Session} and empty
 * {@link Source}
 *
 * @author Iliana Ghazali
 */
@EnableScheduling
public class ManagerCleanScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerCleanScheduler.class);

    public static final Long MAX_TASK_DELAY = 60L; // In second

    public static final String CLEAN_SESSION = "Clean session, session steps and unused sources";

    public static final String CLEAN_SESSION_TITLE = "Clean session scheduling";

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
    private String microserviceName;

    /**
     * Snapshot task
     */
    private final LockingTaskExecutor.Task cleanProcessTask = () -> {
        LockAssert.assertLocked();
        managerCleanJobService.scheduleJob();
    };

    /**
     * Clean expired sessions the first sunday of every month at midnight. Wait for {@link ManagerSnapshotJob} ending
     * to avoid any conflicts, as both of these process handle the same objects.
     */
    @Scheduled(cron = "${regards.session.manager.clean.session.cron:0 0 0 1-7 * SUN}")
    public void scheduleCleanSession() {
       scheduleJob();
    }

    /**
     * Schedule {@link ManagerCleanJob}
     */
    public void scheduleJob() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, CLEAN_SESSION);
                long startTime = System.currentTimeMillis();
                boolean isSnapshotJobsFinished = waitUntilManagerSnapshotJobEnd(startTime);
                if (isSnapshotJobsFinished) {
                    lockingTaskExecutors.executeWithLock(cleanProcessTask,
                                                         new LockConfiguration(microserviceName + "_clean-session",
                                                                               Instant.now()
                                                                                       .plusSeconds(MAX_TASK_DELAY)));
                } else {
                    LOGGER.warn("[MANAGER CLEAN SESSION SCHEDULER] - {} could not be executed because "
                                        + "AgentSnapshotJobs did not finished on time. Waited for {}ms.",
                                CLEAN_SESSION_TITLE, System.currentTimeMillis() - startTime);
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

    /**
     * Wait for {@link ManagerSnapshotJob}s in states {@link JobStatus#QUEUED}, {@link JobStatus#PENDING} or
     * {@link JobStatus#RUNNING} to end
     * @param startTime beginning of the wait
     * @return if {@link ManagerSnapshotJob}s have ended before the end of the wait
     */
    private boolean waitUntilManagerSnapshotJobEnd(long startTime) {
        // wait 5 minutes until all AgentSnapshotJobs end
        final long waitDuration = 300000L;
        final long sleepDuration = 45000L;
        long count;
        long currentWait;
        long maxWait = startTime + waitDuration;

        do {
            count = this.jobService
                    .retrieveJobsCount(ManagerSnapshotJob.class.getName(), JobStatus.QUEUED, JobStatus.PENDING,
                                       JobStatus.RUNNING);
            currentWait = System.currentTimeMillis();
            // wait 45s if jobs are currently running
            if (count != 0L) {
                LOGGER.info("[MANAGER CLEAN SESSION SCHEDULER] Waiting for ManagerSnapshotJobs ending to start "
                                    + "ManagerCleanJob ... Current number of {} in running, pending or queued "
                                    + "states {}.", ManagerSnapshotJob.class.getName(), count);
                try {
                    Thread.sleep(sleepDuration);
                } catch (InterruptedException e) {
                    LOGGER.warn("[MANAGER CLEAN SESSION SCHEDULER] - the thread was interrupted while waiting for "
                                        + "ManagerSnapshotJob ending", e);
                    // Restore interrupted state
                    Thread.currentThread().interrupt();
                }
            }
        } while (count != 0L && currentWait < maxWait && !Thread.currentThread().isInterrupted());
        return count == 0L;
    }
}