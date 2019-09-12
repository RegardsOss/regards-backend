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
package fr.cnes.regards.modules.storagelight.service.file.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.locks.service.ILockService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;

/**
 * Scheduler to periodically handle bulk requests for storage and deletion on storage locations.<br />
 * Those requests are <ul>
 * <li> {@link FileStorageRequest} for storage</li>
 * <li> {@link FileDeletionRequest} for deletion</li>
 * </ul>
 * @author Sébastien Binda
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class FileRequestScheduler {

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private FileStorageRequestService fileStorageRequestService;

    @Autowired
    private FileDeletionRequestService fileDeletionRequestService;

    @Autowired
    private FileCacheRequestService fileCacheRequestService;

    @Autowired
    private FileCopyRequestService FileCopyRequestService;

    @Autowired
    private ILockService lockService;

    /**
     * Number of created AIPs processed on each iteration by project
     */
    @Value("${regards.storage.aips.iteration.limit:100}")
    private Integer aipIterationLimit;

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:3000}", initialDelay = 1_000)
    public void handleFileStorageRequests() throws ModuleException {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                if (getLock()) {
                    fileStorageRequestService.scheduleJobs(FileRequestStatus.TODO, Sets.newHashSet(),
                                                           Sets.newHashSet());
                }
            } finally {
                releaseLock();
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:3000}", initialDelay = 1_000)
    public void handleFileCacheRequests() throws ModuleException {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            if (getLock()) {
                try {
                    fileCacheRequestService.scheduleJobs(FileRequestStatus.TODO);
                } finally {
                    releaseLock();
                    runtimeTenantResolver.clearTenant();
                }
            }
        }

    }

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:3000}", initialDelay = 1_000)
    public void handleFileDeletionRequests() throws ModuleException {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            if (getLock()) {
                try {
                    fileDeletionRequestService.scheduleJobs(FileRequestStatus.TODO, Sets.newHashSet());
                } finally {
                    releaseLock();
                    runtimeTenantResolver.clearTenant();
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:3000}", initialDelay = 1_000)
    public void handleFileCopyRequests() throws ModuleException {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            if (getLock()) {
                try {
                    FileCopyRequestService.scheduleAvailabilityRequests(FileRequestStatus.TODO);
                } finally {
                    releaseLock();
                    runtimeTenantResolver.clearTenant();
                }
            }
        }
    }

    /**
     * Get lock to ensure schedulers are not started at the same time by many instance of this microservice
     * @return
     */
    private boolean getLock() {
        return lockService.obtainLockOrSkip(this.getClass().getName(), this, 60_000);
    }

    /**
     * Release lock
     */
    private void releaseLock() {
        lockService.releaseLock(this.getClass().getName(), this);
    }
}
