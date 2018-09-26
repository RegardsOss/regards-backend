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
package fr.cnes.regards.modules.storage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.database.CachedFile;
import fr.cnes.regards.modules.storage.domain.database.CachedFileState;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * Enable storage task schedulers.
 *
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Component
@Profile("!disableStorageTasks")
@EnableScheduling
public class ScheduleStorageTasks {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleStorageTasks.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private ICachedFileService cachedFileService;

    @Autowired
    private IDataStorageService dataStorageService;

    /**
     * Number of created AIPs processed on each iteration by project
     */
    @Value("${regards.storage.aips.iteration.limit:100}")
    private Integer aipIterationLimit;

    @Scheduled(fixedDelayString = "${regards.storage.store.delay:60000}", initialDelay = 10000)
    public void store() throws ModuleException {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                long startTime = System.currentTimeMillis();
                // Extract data files from valid AIP (microservice concurrent action)
                Pageable page = new PageRequest(0, aipIterationLimit);
                Page<AIP> createdAips;
                do {
                    createdAips = aipService.storePage(page);
                    page = createdAips.nextPageable();
                } while (createdAips.hasNext());

                LOGGER.trace("AIP data scheduled in {}ms", System.currentTimeMillis() - startTime);

            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * This cron action executed every minutes handle update of {@link AIP} state by
     * looking for all associated {@link StorageDataFile} states. An {@link AIP} is STORED
     * when all his {@link StorageDataFile}s are STORED.
     */
    @Scheduled(fixedDelayString = "${regards.storage.check.aip.metadata.delay:30000}")
    public void storeMetadata() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            aipService.storeMetadata();
            runtimeTenantResolver.clearTenant();
        }
    }

    /*
     * Non javadoc, but explanatory: due to settings only interfaces are proxyfied by spring, so we need to use a self
     * reference on the interface to profit from transaction management from spring. This is a self reference because
     * AIPService is annotated @Service with default component scope which is "spring' SINGLETON
     */
    @Scheduled(fixedDelayString = "${regards.storage.update.aip.metadata.delay:120000}") // 2 minutes
    public void updateAlreadyStoredMetadata() {
        // Then lets get AIP that should be stored again after an update
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            aipService.updateAipMetadata();
            aipService.removeDeletedAIPMetadatas();
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Periodicaly delete AIPs metadata in status DELETED. Delete physical file and reference in database.
     */
    @Scheduled(fixedDelayString = "${regards.storage.delete.aip.metadata.delay:120000}") // 2 minutes
    public void deleteMetadata() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            aipService.removeDeletedAIPMetadatas();
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Periodicaly delete AIPs data in status TO_BE_DELETED. Delete physical file and reference in database.
     */
    @Scheduled(fixedDelayString = "${regards.storage.delete.aip.data.delay:120000}") // 2 minutes
    public void deleteData() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            aipService.doDelete();
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Periodicly check the cache total size and delete expired files or/and older files if needed.
     * Default : scheduled to be run every 5minutes.
     */
    @Scheduled(fixedRateString = "${regards.cache.cleanup.rate.ms:300000}")
    public void cleanCache() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            cachedFileService.purge();
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
            cachedFileService.restoreQueued();
            runtimeTenantResolver.clearTenant();
        }
    }

    @Scheduled(fixedRateString = "${regards.storage.check.data.storage.disk.usage.rate:3600000}",
            initialDelay = 60 * 1000)
    public void monitorDataStorages() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            dataStorageService.monitorDataStorages();
            runtimeTenantResolver.clearTenant();
        }
    }
}
