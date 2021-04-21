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
package fr.cnes.regards.modules.storage.service.location;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.domain.database.StorageLocation;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;

/**
 * Enable storage task schedulers.
 * This component run multiple scheduled and periodically executed methods :
 * <ul>
 * <li> Monitor storage locations {@link StorageLocation} </li>
 * </ul>
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 * @author Binda Sébastien
 *
 */
@Component
@Profile({ "!noschedule" })
@EnableScheduling
public class StorageLocationScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageLocationScheduler.class);

    private static final String FILE_LOCATION_SCHEDULER_LOCK = "file_location_schedule_lock";

    private static final String DEFAULT_INITIAL_DELAY = "10000";

    private static final String DEFAULT_DELAY = "3600000";

    private static final String MONITOR_TITLE = "Monitoring storage location scheduling";

    private static final String MONITOR_ACTIONS = "MONITORING STORAGE LOCATION ACTIONS";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    @Value("${regards.storage.location.full.calculation.ratio:20}")
    private Integer fullCalculationRatio;

    private static int lightCalculationCount = 0;

    private static boolean reset = false;

    private final Task monitorTask = () -> {
        LockAssert.assertLocked();
        long startTime = System.currentTimeMillis();
        storageLocationService.monitorStorageLocations(reset);
        LOGGER.trace("Data storages monitoring done in {}ms", System.currentTimeMillis() - startTime);
    };

    @Scheduled(initialDelayString = "${regards.storage.location.schedule.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
            fixedDelayString = "${regards.storage.location.schedule.delay:" + DEFAULT_DELAY + "}")
    public void scheduleUpdateRequests() {
        if (lightCalculationCount > fullCalculationRatio) {
            lightCalculationCount = 0;
            reset = true;
        } else {
            lightCalculationCount++;
            reset = false;
        }
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, MONITOR_ACTIONS);
                lockingTaskExecutors.executeWithLock(monitorTask, new LockConfiguration(FILE_LOCATION_SCHEDULER_LOCK,
                        Instant.now().plusSeconds(120)));
            } catch (Throwable e) {
                handleSchedulingError(MONITOR_ACTIONS, MONITOR_TITLE, e);
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