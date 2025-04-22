/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.filecatalog.service.scheduler;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.filecatalog.service.location.StorageLocationMonitoringService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduler to periodically monitor storage locations.
 *
 * @author Thibaud Michaudel
 **/
@Component
@Profile("!noscheduler")
@EnableScheduling
public class MonitorStorageLocationScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorStorageLocationScheduler.class);

    private static final String DEFAULT_INITIAL_DELAY_IN_MS = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY_IN_MS = "3600000"; // 1 hour

    private static final String LOCK_ID = "file-catalog-storage-location-monitoring";

    private static final String LOCK_TITLE = "File Catalog storage location monitoring";

    private static final String LOCK_ACTIONS = "FILE CATALOG STORAGE LOCATION MONITORING ACTIONS";

    private final LockingTaskExecutor.Task monitorStorageLocationTask;

    private final ILockingTaskExecutors lockingTaskExecutors;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ITenantResolver tenantResolver;

    private final StorageLocationMonitoringService storageLocationMonitoringService;

    @Value("${regards.file.catalog.monitor.storage.location.lock.duration:300}")
    private int lockDuration;

    public MonitorStorageLocationScheduler(ILockingTaskExecutors lockingTaskExecutors,
                                           IRuntimeTenantResolver runtimeTenantResolver,
                                           ITenantResolver tenantResolver,
                                           StorageLocationMonitoringService storageLocationMonitoringService) {
        monitorStorageLocationTask = () -> {
            lockingTaskExecutors.assertLocked();
            storageLocationMonitoring();
        };
        this.lockingTaskExecutors = lockingTaskExecutors;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
        this.storageLocationMonitoringService = storageLocationMonitoringService;
    }

    public void storageLocationMonitoring() {
        try {
            storageLocationMonitoringService.monitorStorageLocations(false);
        } catch (ModuleException e) {
            LOGGER.error("Error while monitoring storage locations: {}", e.getMessage(), e);
        }
    }

    @Scheduled(initialDelayString = "${regards.file.catalog.schedule.initial.delay.ms:"
                                    + DEFAULT_INITIAL_DELAY_IN_MS
                                    + "}",
               fixedDelayString = "${regards.file.catalog.schedule.delay.ms:" + DEFAULT_SCHEDULING_DELAY_IN_MS + "}")
    private void scheduleStorageLocationMonitoring() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, LOCK_ACTIONS);
                lockingTaskExecutors.executeWithLock(monitorStorageLocationTask,
                                                     new LockConfiguration(Instant.now(),
                                                                           LOCK_ID,
                                                                           Duration.ofSeconds(lockDuration),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(LOCK_ACTIONS, LOCK_TITLE, e);
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