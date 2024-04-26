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
package fr.cnes.regards.framework.modules.session.agent.service.update;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Scheduler to launch {@link AgentSnapshotJob}
 *
 * @author Iliana Ghazali
 */
@Profile({ "!noscheduler" })
@Component
@ConditionalOnProperty(value = "regards.microservices.agent.snapshot.enabled", matchIfMissing = true)
@EnableScheduling
public class AgentSnapshotScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSnapshotScheduler.class);

    public static final Long MAX_TASK_DELAY = 60L; // In second

    public static final String DEFAULT_INITIAL_DELAY = "10000";

    public static final String DEFAULT_SCHEDULING_DELAY = "5000"; // every 5 seconds

    public static final String SNAPSHOT_PROCESS = "Session Step Snapshot";

    public static final String SNAPSHOT_PROCESS_TITLE = "Snapshot process scheduling";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private AgentSnapshotJobService agentSnapshotJobService;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Snapshot task
     */
    private final LockingTaskExecutor.Task snapshotProcessTask = () -> {
        lockingTaskExecutors.assertLocked();
        agentSnapshotJobService.scheduleJob();
    };

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(initialDelayString = "${regards.session.agent.snapshot.process.scheduler.bulk.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY
                                    + "}",
               fixedDelayString = "${regards.session.agent.snapshot.process.scheduler.bulk.delay:"
                                  + DEFAULT_SCHEDULING_DELAY
                                  + "}")
    protected void scheduleAgentSnapshot() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, SNAPSHOT_PROCESS);
                lockingTaskExecutors.executeWithLock(snapshotProcessTask,
                                                     new LockConfiguration(microserviceName + "_session-agent-snapshot",
                                                                           Instant.now().plusSeconds(MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(SNAPSHOT_PROCESS, SNAPSHOT_PROCESS_TITLE, e);
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
