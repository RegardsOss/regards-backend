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

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static fr.cnes.regards.modules.ingest.service.schedule.SchedulerConstant.*;

/**
 * Scheduler to periodically check if there is some pending request that can be scheduled
 * <p>
 * NOTE : Number of parallel schedule execution is defined by spring configuration property spring.task.scheduling.pool.size
 *
 * @author Léo Mieulet
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class RequestPendingScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestPendingScheduler.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IRequestService requestService;

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    private final Task unlockRequestsTask = () -> {
        LockAssert.assertLocked();
        requestService.unblockRequests(RequestTypeEnum.AIP_UPDATES_CREATOR);
        requestService.unblockRequests(RequestTypeEnum.OAIS_DELETION);
        requestService.unblockRequests(RequestTypeEnum.OAIS_DELETION_CREATOR);
        requestService.unblockRequests(RequestTypeEnum.UPDATE);
    };

    @Scheduled(initialDelayString = "${regards.ingest.schedule.pending.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
        fixedDelayString = "${regards.ingest.schedule.pending.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleUpdateRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, UNLOCK_ACTIONS);
                lockingTaskExecutors.executeWithLock(unlockRequestsTask,
                                                     new LockConfiguration(UNLOCK_REQ_SCHEDULER_LOCK,
                                                                           Instant.now().plusSeconds(120)));
            } catch (Throwable e) {
                handleSchedulingError(UNLOCK_ACTIONS, UNLOCK_TITLE, e);
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
