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
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    private FileCopyRequestService fileCopyRequestService;

    @Autowired
    private ILockService lockService;

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:3000}", initialDelay = 10_000)
    @Transactional(propagation = Propagation.NEVER)
    public void handleFileStorageRequests() throws ModuleException {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                if (obtainLock()) {
                    fileStorageRequestService.scheduleJobs(FileRequestStatus.TODO, Sets.newHashSet(),
                                                           Sets.newHashSet());
                }
            } finally {
                releaseLock();
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:3000}", initialDelay = 11_000)
    @Transactional(propagation = Propagation.NEVER)
    public void handleFileCacheRequests() throws ModuleException {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            if (obtainLock()) {
                try {
                    fileCacheRequestService.scheduleJobs(FileRequestStatus.TODO);
                } finally {
                    releaseLock();
                    runtimeTenantResolver.clearTenant();
                }
            }
        }

    }

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:3000}", initialDelay = 12_000)
    @Transactional(propagation = Propagation.NEVER)
    public void handleFileDeletionRequests() throws ModuleException {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            if (obtainLock()) {
                try {
                    fileDeletionRequestService.scheduleJobs(FileRequestStatus.TODO, Sets.newHashSet());
                } finally {
                    releaseLock();
                    runtimeTenantResolver.clearTenant();
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:3000}", initialDelay = 13_000)
    @Transactional(propagation = Propagation.NEVER)
    public void handleFileCopyRequests() throws ModuleException {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            if (obtainLock()) {
                try {
                    fileCopyRequestService.scheduleAvailabilityRequests(FileRequestStatus.TODO);
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
    private boolean obtainLock() {
        return lockService.obtainLockOrSkip(this.getClass().getName(), this, 60L);
    }

    /**
     * Release lock
     */
    private void releaseLock() {
        lockService.releaseLock(this.getClass().getName(), this);
    }
}
