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
import fr.cnes.regards.modules.filecatalog.service.FileStorageRequestService;
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

/**
 * Scheduler to check if a File Storage Request need to be processed or if the FileReference already exists. This
 * scheduler will also delete completed requests.
 *
 * @see FilesStorageRequestEventHandler
 **/
@Component
@Profile("!noscheduler")
@EnableScheduling
public class FileStorageRequestCheckScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageRequestCheckScheduler.class);

    private static final String DEFAULT_INITIAL_DELAY_IN_MS = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY_IN_MS = "1000";

    private static final String FILE_CATALOG_LOCK = "file-catalog-requests-check";

    private static final String FILE_CATALOG_TITLE = "File Catalog requests check scheduling";

    private static final String FILE_CATALOG_ACTIONS = "FILE CATALOG REQUESTS CHECKING ACTIONS";

    private final ILockingTaskExecutors lockingTaskExecutors;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ITenantResolver tenantResolver;

    private final LockingTaskExecutor.Task handleCheckRequestsTask;

    private final FileStorageRequestService fileStorageRequestService;

    @Value("${regards.file.catalog.files.storage.request.check.scheduler.lock.duration:300}")
    private int lockDuration;

    public FileStorageRequestCheckScheduler(ILockingTaskExecutors lockingTaskExecutors,
                                            IRuntimeTenantResolver runtimeTenantResolver,
                                            ITenantResolver tenantResolver,
                                            FileStorageRequestService fileStorageRequestService) {
        this.lockingTaskExecutors = lockingTaskExecutors;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
        this.handleCheckRequestsTask = () -> {
            lockingTaskExecutors.assertLocked();
            handleFileStorageCheckRequests();
            deleteRequests();
        };
        this.fileStorageRequestService = fileStorageRequestService;
    }

    public void handleFileStorageCheckRequests() {
        // Get all requests in GRANTED status
        long start = System.currentTimeMillis();
        LOGGER.trace("[STORAGE REQUESTS] Checking storage requests ...");
        Pageable page = PageRequest.of(0, FileStorageRequestService.SMALL_PAGE_SIZE, Sort.by("id"));
        do {
            page = fileStorageRequestService.doCheckRequests(page);
        } while (page.isPaged());
        LOGGER.debug("[STORAGE REQUESTS] Requests checked in {} ms", System.currentTimeMillis() - start);
    }

    public void deleteRequests() {
        fileStorageRequestService.deleteRequests();
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
                lockingTaskExecutors.executeWithLock(handleCheckRequestsTask,
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
