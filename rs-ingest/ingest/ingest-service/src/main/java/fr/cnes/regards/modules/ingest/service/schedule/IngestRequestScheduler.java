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
package fr.cnes.regards.modules.ingest.service.schedule;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.service.aip.scheduler.IngestRequestSchedulerService;

import static fr.cnes.regards.modules.ingest.service.schedule.SchedulerConstant.*;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;

/**
 * Scheduler for IngestRequest
 * This scheduler will look for {@link IngestRequest} with state
 * {@link InternalRequestState#TO_SCHEDULE} and either schedule (CREATED) them or block (BLOCKED) them.
 * <p>
 * A request is scheduled if there is no other request with the same providerId already CREATED or RUNNING,
 * and if it is the oldest request sharing this providerId.
 * To check if the request is the oldest, the submissionDate is used if it is present and the creationDate is used otherwise.
 * <p>
 * This scheduler does not manage the change of state between BLOCKED and TO_SCHEDULE, this is handled by {@link RequestPendingScheduler}.
 *
 * @author Thibaud Michaudel
 */
@Component
@Profile({ "!noscheduler" })
@EnableScheduling
public class IngestRequestScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestRequestScheduler.class);

    private final ITenantResolver tenantResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private ILockingTaskExecutors lockingTaskExecutors;

    private IngestRequestSchedulerService ingestRequestSchedulerService;

    private final Long schedlockTimoutSeconds;

    /**
     * Create Ingest Request Task
     */
    private final LockingTaskExecutor.Task createIngestRequestTask = () -> {
        lockingTaskExecutors.assertLocked();
        long start = System.currentTimeMillis();
        // Only schedule first page of ingest request to avoid one tenant using all resources.
        ingestRequestSchedulerService.scheduleFirstPageRequests();
        LOGGER.debug("[INGEST REQUEST TASK SCHEDULER] Scheduler handled in {} ms", System.currentTimeMillis() - start);
    };

    public IngestRequestScheduler(ITenantResolver tenantResolver,
                                  IRuntimeTenantResolver runtimeTenantResolver,
                                  ILockingTaskExecutors lockingTaskExecutors,
                                  IngestRequestSchedulerService ingestRequestSchedulerService,
                                  @Value("${regards.ingest.schedlock.timeout:120}") Long schedlockTimoutSeconds) {
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.lockingTaskExecutors = lockingTaskExecutors;
        this.ingestRequestSchedulerService = ingestRequestSchedulerService;
        this.schedlockTimoutSeconds = schedlockTimoutSeconds;
    }

    @Scheduled(initialDelayString = "${regards.ingest.schedule.request.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
               fixedDelayString = "${regards.ingest.schedule.request.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void launchScheduleIngestRequestsTask() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, INGEST_REQUEST_CREATE);
                lockingTaskExecutors.executeWithLock(createIngestRequestTask,
                                                     new LockConfiguration(Instant.now(),
                                                                           INGEST_REQUEST_CREATE_LOCK,
                                                                           Duration.ofSeconds(schedlockTimoutSeconds),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(INGEST_REQUEST_CREATE, INGEST_REQUEST_CREATE_LOCK, e);
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
