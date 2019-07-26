/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storagelight.domain.database.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.database.StorageLocation;

/**
 * Enable storage task schedulers.
 * This component run multiple scheduled and periodically executed methods :
 * <ul>
 * <li> Handle file reference request {@link FileStorageRequest} </li>
 * <li> Monitor storage locations {@link StorageLocation} </li>
 * <li> Cache purge: {@link #cleanCache()}</li>
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

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private StorageLocationService storageLocationService;

    @Scheduled(fixedRateString = "${regards.storage.check.data.storage.disk.usage.rate:3600000}",
            initialDelay = 60 * 1000)
    public void monitorDataStorages() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            long startTime = System.currentTimeMillis();
            storageLocationService.monitorDataStorages();
            LOGGER.trace("Data storages monitoring done in {}ms", System.currentTimeMillis() - startTime);
            runtimeTenantResolver.clearTenant();
        }
    }
}
