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
package fr.cnes.regards.modules.storagelight.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * Enable storage task schedulers.
 * This component run multiple scheduled and periodically executed methods :
 * <ul>
 * <li>Cache purge: {@link #cleanCache()}</li>
 * <li>Handle queued file: {@link #restoreToCache()}</li>
 * <li>AIP data deletion: {@link #deleteData()}</li>
 * <li>AIP metadata file deletion: {@link #deleteMetadata()}</li>
 * <li>Data storage monitoring: {@link #monitorDataStorages()}</li>
 * <li>AIP data storage: {@link #store()}</li>
 * <li>AIP metadata storage: {@link #storeMetadata()}</li>
 * <li>AIP metadata updates: {@link #handleUpdateRequests()}</li>
 * </ul>
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Component
@Profile("!disableStorageTasks")
@EnableScheduling
public class Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private FileReferenceService fileReferenceService;

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private CachedFileService cachedFileService;

    /**
     * Number of created AIPs processed on each iteration by project
     */
    @Value("${regards.storage.aips.iteration.limit:100}")
    private Integer aipIterationLimit;

    @Scheduled(fixedDelayString = "${regards.storage.store.delay:5000}", initialDelay = 10000)
    public void handleFileReferenceRequests() throws ModuleException {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                fileReferenceService.scheduleStoreJobs();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Periodically check the cache total size and delete expired files or/and older files if needed.
     * Default : scheduled to be run every 5minutes.
     */
    @Scheduled(fixedRateString = "${regards.cache.cleanup.rate.ms:300000}")
    public void cleanCache() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            long startTime = System.currentTimeMillis();
            int nbPurged = cachedFileService.purge();
            LOGGER.trace("Cache clean done in {}ms for {} files", System.currentTimeMillis() - startTime, nbPurged);
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Periodically tries to restore all {@link CachedFile}s in {@link CachedFileState#QUEUED} status.
     * Default : scheduled to be run every 2minutes.
     */
    @Scheduled(fixedRateString = "${regards.cache.restore.queued.rate.ms:120000}")
    public void restoreToCache() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            long startTime = System.currentTimeMillis();
            int nbScheduled = cachedFileService.restoreQueued();
            LOGGER.trace("Cache restoration done in {}ms for {} files", System.currentTimeMillis() - startTime,
                         nbScheduled);
            runtimeTenantResolver.clearTenant();
        }
    }

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
