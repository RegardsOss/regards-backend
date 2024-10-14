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
package fr.cnes.regards.modules.storage.service.location;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.domain.database.StorageLocation;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;
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
 * Enable storage task schedulers.
 * This component run multiple scheduled and periodically executed methods :
 * <ul>
 * <li> Monitor storage locations {@link StorageLocation} </li>
 * </ul>
 *
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 * @author Binda Sébastien
 */
@Component
@Profile({ "!noscheduler" })
@EnableScheduling
public class StorageLocationScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageLocationScheduler.class);

    private static final String FILE_LOCATION_SCHEDULER_LOCK = "file_location_schedule_lock";

    private static final long FILE_LOCATION_LOCK_TIME_TO_LIVE_IN_SECONDS = 60;

    private static final String DEFAULT_INITIAL_DELAY = "10000";

    private static final String DEFAULT_DELAY = "3600000";

    private static final String DEFAULT_PERIODIC_TASKS_CRON = "0 0 5 * * ?";

    private static final String MONITOR_TITLE = "Monitoring storage location scheduling";

    private static final String MONITOR_ACTIONS = "MONITORING STORAGE LOCATION ACTIONS";

    private static final String STORE_PENDING_ACTION = "STORE PENDING REMAINING ACTIONS";

    private static final String STORE_PENDING_TITLE = "Run storage remaining pending actions";

    private static final String STORE_PENDING_ACTION_SCHEDULER_LOCK = "pending_action_remaining_schedule_lock";

    private static int lightCalculationCount = 0;

    private static boolean reset = false;

    private ILockingTaskExecutors lockingTaskExecutors;

    private final Integer fullCalculationRatio;

    private final ITenantResolver tenantResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private StorageLocationService storageLocationService;

    private final Task monitorTask = () -> {
        lockingTaskExecutors.assertLocked();
        long startTime = System.currentTimeMillis();
        storageLocationService.monitorStorageLocations(reset);
        LOGGER.trace("Data storages monitoring done in {}ms", System.currentTimeMillis() - startTime);
    };

    private final Task storagePeriodicActionTask = () -> {
        lockingTaskExecutors.assertLocked();
        long startTime = System.currentTimeMillis();
        storageLocationService.runPeriodicTasks();
        LOGGER.info("Periodic task on storages done in {}ms", System.currentTimeMillis() - startTime);
    };

    public StorageLocationScheduler(ITenantResolver tenantResolver,
                                    IRuntimeTenantResolver runtimeTenantResolver,
                                    StorageLocationService storageLocationService,
                                    ILockingTaskExecutors lockingTaskExecutors,
                                    @Value("${regards.storage.location.full.calculation.ratio:20}")
                                    Integer fullCalculationRatio) {
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.storageLocationService = storageLocationService;
        this.lockingTaskExecutors = lockingTaskExecutors;
        this.fullCalculationRatio = fullCalculationRatio;
    }

    @Scheduled(initialDelayString = "${regards.storage.location.schedule.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
               fixedDelayString = "${regards.storage.location.schedule.delay:" + DEFAULT_DELAY + "}")
    public void scheduleMonitorStorageLocations() {
        if (lightCalculationCount > fullCalculationRatio) {
            lightCalculationCount = 0;
            reset = true;
        } else {
            lightCalculationCount++;
            reset = false;
        }
        scheduleForAllTenants(MONITOR_ACTIONS, monitorTask, FILE_LOCATION_SCHEDULER_LOCK, MONITOR_TITLE);
    }

    @Scheduled(cron = "${regards.storage.location.periodic.tasks.cron:" + DEFAULT_PERIODIC_TASKS_CRON + "}")
    public void schedulePeriodicActionOnStorages() {
        scheduleForAllTenants(STORE_PENDING_ACTION,
                              storagePeriodicActionTask,
                              STORE_PENDING_ACTION_SCHEDULER_LOCK,
                              STORE_PENDING_TITLE);
    }

    private void scheduleForAllTenants(String actionLabel, Task task, String lockId, String schedulerTitle) {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, actionLabel);
                lockingTaskExecutors.executeWithLock(task,
                                                     new LockConfiguration(Instant.now(),
                                                                           lockId,
                                                                           Duration.ofSeconds(FILE_LOCATION_LOCK_TIME_TO_LIVE_IN_SECONDS),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(actionLabel, schedulerTitle, e);
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