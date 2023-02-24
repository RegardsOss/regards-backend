/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.storage.domain.database.request.*;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Scheduler to periodically handle bulk requests :<br />
 * <li> {@link FileStorageRequest} for storage</li>
 * <li> {@link FileDeletionRequest} for deletion</li>
 * <li> {@link FileCopyRequest} for copy</li>
 * <li> {@link FileCacheRequest} for availability</li>
 * </ul>
 * <p>
 * NOTE : Number of parallel schedule execution is defined by spring configuration property spring.task.scheduling.pool.size.
 *
 * @author Sébastien Binda
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class FileRequestScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRequestScheduler.class);

    private static final String STORAGE_LOCK = "storage-requests";

    private static final String STORAGE_TITLE = "Storage requests scheduling";

    private static final String STORAGE_ACTIONS = "STORAGE REQUESTS ACTIONS";

    private static final String DEFAULT_INITIAL_DELAY = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY = "1000";

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
    private RequestStatusService reqStatusService;

    private final Task handleRequestsTask = () -> {
        LockAssert.assertLocked();
        handleGroupRequests();
        handleExpiredCacheRequests();
        handleFileCacheRequests();
        handleFileStorageRequests();
        handleFileDeletionRequests();
        handleFileCopyRequests();
    };

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    public void handleFileStorageRequests() {
        reqStatusService.checkDelayedStorageRequests();
        fileStorageRequestService.scheduleJobs(FileRequestStatus.TO_DO, Sets.newHashSet(), Sets.newHashSet());
    }

    public void handleFileCacheRequests() {
        reqStatusService.checkDelayedCacheRequests();
        fileCacheRequestService.scheduleJobs(FileRequestStatus.TO_DO);
    }

    private void handleExpiredCacheRequests() {
        fileCacheRequestService.cleanExpiredCacheRequests();
    }

    public void handleFileDeletionRequests() {
        reqStatusService.checkDelayedDeleteRequests();
        fileDeletionRequestService.scheduleJobs(FileRequestStatus.TO_DO, Sets.newHashSet());
    }

    public void handleFileCopyRequests() {
        reqStatusService.checkDelayedCopyRequests();
        fileCopyRequestService.scheduleCopyRequests(FileRequestStatus.TO_DO);
    }

    public void handleGroupRequests() {
        reqGrpService.checkRequestsGroupsDone();
    }

    @Scheduled(initialDelayString = "${regards.storage.schedule.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
               fixedDelayString = "${regards.storage.schedule.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleUpdateRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, STORAGE_ACTIONS);
                lockingTaskExecutors.executeWithLock(handleRequestsTask,
                                                     new LockConfiguration(STORAGE_LOCK,
                                                                           Instant.now().plusSeconds(1200)));
            } catch (Throwable e) {
                handleSchedulingError(STORAGE_ACTIONS, STORAGE_TITLE, e);
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
