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
package fr.cnes.regards.framework.modules.session.agent.service.clean.snapshotprocess;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
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
 * Scheduler to clean not used {@link fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess}
 *
 * @author Iliana Ghazali
 */
@EnableScheduling
public class AgentCleanSnapshotProcessScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentCleanSnapshotProcessScheduler.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private AgentCleanSnapshotProcessJobService agentCleanSnapshotProcessJobService;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    @Value("${spring.application.name}")
    private String microserviceName;

    public static final Long MAX_TASK_DELAY = 60L; // In second

    public static final String DEFAULT_INITIAL_DELAY = "10000";

    public static final String DEFAULT_SCHEDULING_DELAY = "2629800000"; // once a month

    public static final String DEFAULT_DEAD_JOBS_SCHEDULING_DELAY = "600000"; // one every 10 minutes

    public static final String CLEAN_SNAPSHOT_PROCESS = "Clean snapshot process";

    public static final String CLEAN_SNAPSHOT_PROCESS_TITLE = "Clean snapshot process scheduling";

    public static final String CLEAN_DEAD_JOBS_PROCESS_TITLE = "Clean dead jobs process scheduling";

    public static final String CLEAN_DEAD_JOBS_PROCESS = "Clean snapshot process";

    /**
     * Snapshot task
     */
    private final LockingTaskExecutor.Task cleanSnapshotProcessTask = () -> {
        lockingTaskExecutors.assertLocked();
        agentCleanSnapshotProcessJobService.scheduleJob();
    };

    private final LockingTaskExecutor.Task cleanDeadJobsTask = () -> {
        lockingTaskExecutors.assertLocked();
        agentCleanSnapshotProcessJobService.cleanDeadJobs();
    };

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(initialDelayString = "${regards.session.agent.clean.snapshot.process.scheduler.bulk.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY
                                    + "}",
               fixedDelayString = "${regards.session.agent.clean.snapshot.process.scheduler.bulk.delay:"
                                  + DEFAULT_SCHEDULING_DELAY
                                  + "}")
    protected void scheduleCleanSnapshotProcess() {
        runTaskForAllTenants(cleanSnapshotProcessTask, CLEAN_SNAPSHOT_PROCESS_TITLE, CLEAN_SNAPSHOT_PROCESS);
    }

    /**
     * Clean snapshot process to remove dead jobs if any
     */
    @Scheduled(initialDelayString = "${regards.session.agent.clean.snapshot.dead.jobs.scheduler.bulk.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY
                                    + "}",
               fixedDelayString = "${regards.session.agent.clean.snapshot.dead.jobs.scheduler.bulk.delay:"
                                  + DEFAULT_DEAD_JOBS_SCHEDULING_DELAY
                                  + "}")
    protected void scheduleCleanDeadJobs() {
        runTaskForAllTenants(cleanDeadJobsTask, CLEAN_DEAD_JOBS_PROCESS_TITLE, CLEAN_DEAD_JOBS_PROCESS);
    }

    private void runTaskForAllTenants(LockingTaskExecutor.Task task, String taskTitle, String taskType) {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, taskType);
                lockingTaskExecutors.executeWithLock(task,
                                                     new LockConfiguration(Instant.now(),
                                                                           microserviceName + "_clean-snapshot-process",
                                                                           Duration.ofSeconds(MAX_TASK_DELAY),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(taskType, taskTitle, e);
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