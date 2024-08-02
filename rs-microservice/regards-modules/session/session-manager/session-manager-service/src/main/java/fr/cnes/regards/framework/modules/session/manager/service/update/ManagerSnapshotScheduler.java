/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.manager.service.update;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.session.manager.service.clean.session.ManagerCleanJob;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduler to launch {@link ManagerSnapshotJob}
 *
 * @author Iliana Ghazali
 */
@EnableScheduling
public class ManagerSnapshotScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerSnapshotScheduler.class);

    public static final Long MAX_TASK_DELAY = 60L; // In second

    public static final String DEFAULT_INITIAL_DELAY = "10000";

    public static final String DEFAULT_SCHEDULING_DELAY = "2000"; // every 2 seconds

    public static final String MANAGER_SNAPSHOT_PROCESS = "Session manager snapshot";

    public static final String MANAGER_SNAPSHOT_PROCESS_TITLE = "Session manager snapshot scheduling";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ManagerSnapshotJobService managerSnapshotJobService;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    @Autowired
    private IJobInfoService jobInfoService;

    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Snapshot task
     */
    private final LockingTaskExecutor.Task snapshotProcessTask = () -> {
        lockingTaskExecutors.assertLocked();
        managerSnapshotJobService.scheduleJob();
    };

    /**
     * Schedule {@link ManagerSnapshotJob} every 30s to generate sessions and sources from session steps
     */
    @Scheduled(initialDelayString = "${regards.session.management.snapshot.process.scheduler.bulk.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY
                                    + "}",
               fixedDelayString = "${regards.session.management.snapshot.process.scheduler.bulk.delay:"
                                  + DEFAULT_SCHEDULING_DELAY
                                  + "}")
    public void scheduleManagerSnapshot() {
        scheduleJob();
    }

    /**
     * Schedule {@link ManagerSnapshotJob}
     */
    public void scheduleJob() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, MANAGER_SNAPSHOT_PROCESS);
                // check if a clean process is currently running, if not launch ManagerSnapshotTask
                if (this.jobInfoService.retrieveJobsCount(ManagerCleanJob.class.getName(),
                                                          JobStatus.QUEUED,
                                                          JobStatus.PENDING,
                                                          JobStatus.RUNNING) == 0) {
                    lockingTaskExecutors.executeWithLock(snapshotProcessTask,
                                                         new LockConfiguration(Instant.now(),
                                                                               microserviceName
                                                                               + "_session-manager-snapshot",
                                                                               Duration.ofSeconds(MAX_TASK_DELAY),
                                                                               Duration.ZERO));
                } else {
                    LOGGER.warn("{} could not be executed because a ManagerCleanJob is currently running",
                                MANAGER_SNAPSHOT_PROCESS_TITLE);
                }
            } catch (Throwable e) {
                handleSchedulingError(MANAGER_SNAPSHOT_PROCESS, MANAGER_SNAPSHOT_PROCESS_TITLE, e);
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