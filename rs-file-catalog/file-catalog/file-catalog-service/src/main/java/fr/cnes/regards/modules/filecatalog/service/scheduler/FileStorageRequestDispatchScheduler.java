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
package fr.cnes.regards.modules.filecatalog.service.scheduler;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.fileaccess.dto.StorageRequestStatus;
import fr.cnes.regards.modules.filecatalog.dao.IFileStorageRequestAggregationRepository;
import fr.cnes.regards.modules.filecatalog.service.FileStorageRequestService;
import fr.cnes.regards.modules.filecatalog.service.RequestStatusService;
import fr.cnes.regards.modules.filecatalog.service.handler.FilesStorageRequestEventHandler;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Scheduler to dispatch storage request to file-access.
 * This scheduler will send only one request if multiple ones request the storage of the same file on the same storage.
 *
 * @see FilesStorageRequestEventHandler
 **/
@Component
@Profile("!noscheduler")
@EnableScheduling
public class FileStorageRequestDispatchScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageRequestDispatchScheduler.class);

    private static final String DEFAULT_INITIAL_DELAY_IN_MS = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY_IN_MS = "1000";

    private static final String FILE_CATALOG_LOCK = "file-catalog-requests-dispatch";

    private static final String FILE_CATALOG_TITLE = "File Catalog requests dispatch scheduling";

    private static final String FILE_CATALOG_ACTIONS = "FILE CATALOG REQUESTS DISPATCHING ACTIONS";

    private final FileStorageRequestService fileStorageRequestService;

    private final RequestStatusService reqStatusService;

    private final ILockingTaskExecutors lockingTaskExecutors;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ITenantResolver tenantResolver;

    private final LockingTaskExecutor.Task handleRequestsTask;

    private final IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository;

    @Value("${regards.file.catalog.files.storage.request.dispatch.scheduler.lock.duration:300}")
    private int lockDuration;

    public FileStorageRequestDispatchScheduler(FileStorageRequestService fileStorageRequestService,
                                               RequestStatusService reqStatusService,
                                               ILockingTaskExecutors lockingTaskExecutors,
                                               IRuntimeTenantResolver runtimeTenantResolver,
                                               ITenantResolver tenantResolver,
                                               IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository) {
        this.fileStorageRequestService = fileStorageRequestService;
        this.reqStatusService = reqStatusService;
        this.lockingTaskExecutors = lockingTaskExecutors;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
        this.handleRequestsTask = () -> {
            lockingTaskExecutors.assertLocked();
            checkDelayedRequests();
            handleFileStorageRequests();
        };
        this.fileStorageRequestAggregationRepository = fileStorageRequestAggregationRepository;
    }

    public void checkDelayedRequests() {
        reqStatusService.checkDelayedStorageRequests();
    }

    public void handleFileStorageRequests() {
        Set<String> allStorages = fileStorageRequestAggregationRepository.findStoragesByStatus(StorageRequestStatus.TO_HANDLE);
        long start = System.currentTimeMillis();
        LOGGER.trace("[STORAGE REQUESTS] Dispatching storage requests ...");
        for (String storage : allStorages) {
            Pageable page = PageRequest.of(0, FileStorageRequestService.PAGE_SIZE, Sort.by("metaInfo.checksum"));
            do {
                page = fileStorageRequestService.doDispatchRequestsByStorage(storage, page);
            } while (page.isPaged());
        }
        LOGGER.debug("[STORAGE REQUESTS] Requests dispatched in {} ms", System.currentTimeMillis() - start);

    }

    @Scheduled(initialDelayString = "${regards.file.catalog.schedule.initial.delay.ms:"
                                    + DEFAULT_INITIAL_DELAY_IN_MS
                                    + "}",
               fixedDelayString = "${regards.file.catalog.schedule.delay.ms:" + DEFAULT_SCHEDULING_DELAY_IN_MS + "}")
    public void scheduleUpdateRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, FILE_CATALOG_ACTIONS);
                lockingTaskExecutors.executeWithLock(handleRequestsTask,
                                                     new LockConfiguration(Instant.now(),
                                                                           FILE_CATALOG_LOCK,
                                                                           Duration.ofSeconds(lockDuration),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(FILE_CATALOG_ACTIONS, FILE_CATALOG_TITLE, e);
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
