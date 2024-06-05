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
package fr.cnes.regards.modules.ingest.service.schedule;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.service.aip.scheduler.AIPUpdateRequestScheduler;

import static fr.cnes.regards.modules.ingest.service.schedule.SchedulerConstant.*;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;

/**
 * This component scans the AIPUpdateRepo and regroups tasks by aip to update
 *
 * @author Leo Mieulet
 */
@Profile("!noscheduler")
@Component
public class AIPUpdateJobScheduler extends AbstractTaskScheduler {

    public static final Logger LOGGER = LoggerFactory.getLogger(AIPUpdateJobScheduler.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private AIPUpdateRequestScheduler aipUpdateRequestScheduler;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    /**
     * Update task
     */
    private final Task aipUpdateTask = () -> {
        lockingTaskExecutors.assertLocked();
        long start = System.currentTimeMillis();
        aipUpdateRequestScheduler.scheduleJob();
        LOGGER.debug("[INGEST UPDATE REQUEST TASK SCHEDULER] Scheduler handled in {} ms",
                     System.currentTimeMillis() - start);
    };

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(initialDelayString = "${regards.ingest.aip.update.bulk.delay.init:" + DEFAULT_INITIAL_DELAY + "}",
               fixedDelayString = "${regards.ingest.aip.update.bulk.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    protected void scheduleAIPUpdateJobs() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, AIP_UPDATE_REQUESTS);
                lockingTaskExecutors.executeWithLock(aipUpdateTask,
                                                     new LockConfiguration(Instant.now(),
                                                                           AIP_UPDATE_REQUEST_LOCK,
                                                                           Duration.ofSeconds(MAX_TASK_DELAY),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(AIP_UPDATE_REQUESTS, AIP_UPDATE_TITLE, e);
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
