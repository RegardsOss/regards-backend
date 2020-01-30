/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.modules.locks.service.ILockService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.domain.database.StorageLocation;

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
@Profile("!noscheduler")
@EnableScheduling
public class StorageLocationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageLocationScheduler.class);

    private static final String FILE_LOCATION_SCHEDULER_LOCK = "file_location_schedule_lock";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private ILockService lockService;

    @Scheduled(fixedDelayString = "${regards.storage.check.data.storage.disk.usage.rate:60000}",
            initialDelay = 60 * 1000)
    public void monitorDataStorages() {

        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            obtainLock();
            try {
                long startTime = System.currentTimeMillis();
                storageLocationService.monitorStorageLocations(false);
                LOGGER.trace("Data storages monitoring done in {}ms", System.currentTimeMillis() - startTime);
            } finally {
                runtimeTenantResolver.clearTenant();
                releaseLock();
            }
        }
    }

    /**
     * Get lock to ensure schedulers are not started at the same time by many instance of this microservice
     * @return
     */
    private boolean obtainLock() {
        return lockService.obtainLockOrSkip(FILE_LOCATION_SCHEDULER_LOCK, this, 60L);
    }

    /**
     * Release lock
     */
    private void releaseLock() {
        lockService.releaseLock(FILE_LOCATION_SCHEDULER_LOCK, this);
    }
}