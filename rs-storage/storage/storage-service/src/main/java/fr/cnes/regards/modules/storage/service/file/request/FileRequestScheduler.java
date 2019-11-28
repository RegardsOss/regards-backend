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
package fr.cnes.regards.modules.storage.service.file.request;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.locks.service.ILockService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;

/**
 * Scheduler to periodically handle bulk requests :<br />
 * <li> {@link FileStorageRequest} for storage</li>
 * <li> {@link FileDeletionRequest} for deletion</li>
 * <li> {@link FileCopyRequest} for copy</li>
 * <li> {@link FileCacheRequest} for availability</li>
 * </ul>
 *
 * NOTE : Number of parallel schedule execution is defined by spring configuration property regards.scheduler.pool.size.
 *
 * @author Sébastien Binda
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class FileRequestScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRequestScheduler.class);

    private static final String FILE_SCHEDULER_LOCK = "file-requests-scheduler-lock";

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
    private RequestsGroupService reqGrpService;

    @Autowired
    private ILockService lockService;

    private final Semaphore semaphore = new Semaphore(1, true);

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:3000}", initialDelay = 1_000)
    public void handleFileStorageRequests() throws ModuleException {
        schedule("handleFileStorageRequests", () -> {
            fileStorageRequestService.scheduleJobs(FileRequestStatus.TO_DO, Sets.newHashSet(), Sets.newHashSet());
            return null;
        });
    }

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:3000}", initialDelay = 1_100)
    public void handleFileCacheRequests() throws ModuleException {
        schedule("handleFileCacheRequests", () -> {
            fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
            return null;
        });
    }

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:3000}", initialDelay = 1_200)
    public void handleFileDeletionRequests() throws ModuleException {
        schedule("handleFileDeletionRequests", () -> {
            fileDeletionRequestService.scheduleJobs(FileRequestStatus.TO_DO, Sets.newHashSet());
            return null;
        });
    }

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:3000}", initialDelay = 1_300)
    public void handleFileCopyRequests() throws ModuleException {
        schedule("handleFileCopyRequests", () -> {
            fileCopyRequestService.scheduleCopyRequests(FileRequestStatus.TO_DO);
            return null;
        });
    }

    @Scheduled(fixedDelayString = "${regards.storage.schedule.delay:1000}", initialDelay = 1_400)
    public void handleGroupRequests() throws ModuleException {
        schedule("handleGroupRequests", () -> {
            reqGrpService.checkRequestsGroupsDone();
            return null;
        });
    }

    private void schedule(String taskName, Callable<Void> func) {
        try {
            semaphore.acquire();
            for (String tenant : tenantResolver.getAllActiveTenants()) {
                runtimeTenantResolver.forceTenant(tenant);
                if (obtainLock()) {
                    try {
                        func.call();
                    } finally {
                        releaseLock();
                        runtimeTenantResolver.clearTenant();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error runing scheduling task {}. Cause : {}", taskName, e.getMessage());
        } finally {
            semaphore.release();
        }
    }

    /**
     * Get lock to ensure schedulers are not started at the same time by many instance of this microservice
     * @return
     */
    private boolean obtainLock() {
        return lockService.obtainLockOrSkip(FILE_SCHEDULER_LOCK, this, 60L);
    }

    /**
     * Release lock
     */
    private void releaseLock() {
        lockService.releaseLock(FILE_SCHEDULER_LOCK, this);
    }
}
