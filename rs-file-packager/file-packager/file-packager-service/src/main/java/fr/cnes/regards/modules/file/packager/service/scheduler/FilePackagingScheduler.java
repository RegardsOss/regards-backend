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
package fr.cnes.regards.modules.file.packager.service.scheduler;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackageStatus;
import fr.cnes.regards.modules.file.packager.domain.PackageReference;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduler that will associate a {@link FileInBuildingPackage
 * FileInBuildingPackage} in
 * {@link FileInBuildingPackageStatus#WAITING_PACKAGE WAITING_PACKAGE
 * status} with a {@link PackageReference PackageReference}
 * (creating it if needed).
 * <p/>
 * This scheduler will also close (set {@link PackageReference} status to
 * {@link fr.cnes.regards.modules.file.packager.domain.PackageReferenceStatus#TO_STORE TO_STORE} ) packages older than
 * the maximal package age.
 *
 * @author Thibaud Michaudel
 **/
@Service
@MultitenantTransactional
@Profile("!noscheduler")
public class FilePackagingScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePackagingScheduler.class);

    private static final String DEFAULT_INITIAL_DELAY_IN_MS = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY_IN_MS = "1000";

    private static final String LOCK_ID = "file-packager-file-packaging";

    private static final String LOCK_TITLE = "File Packager file packaging scheduling";

    private static final String LOCK_ACTIONS = "FILE PACKAGER FILE PACKAGING ACTIONS";

    private final LockingTaskExecutor.Task filePackagingTask;

    private final ILockingTaskExecutors lockingTaskExecutors;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ITenantResolver tenantResolver;

    private final FilePackagerService filePackagerService;

    @Value("${regards.file.packager.file.packaging.scheduler.lock.duration.in.seconds:300}")
    private int lockDurationInSeconds;

    @Value("${regards.file.packager.file.packaging.scheduler.page.size:100}")
    private int pageSize;

    public FilePackagingScheduler(ILockingTaskExecutors lockingTaskExecutors,
                                  IRuntimeTenantResolver runtimeTenantResolver,
                                  ITenantResolver tenantResolver,
                                  FilePackagerService filePackagerService) {
        filePackagingTask = () -> {
            lockingTaskExecutors.assertLocked();
            packageFiles();
            closeOldPackages();
        };
        this.lockingTaskExecutors = lockingTaskExecutors;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
        this.filePackagerService = filePackagerService;
    }

    private void packageFiles() {
        long start = System.currentTimeMillis();
        LOGGER.trace("[FILE PACKAGING SCHEDULER] Checking files waiting for packaging ...");

        Pageable page = PageRequest.of(0, pageSize);
        do {
            filePackagerService.associateFilesToPackage(page);
            page.next();
        } while (page.isPaged());

        LOGGER.trace("[FILE PACKAGING SCHEDULER] Requests packaged in {} ms", System.currentTimeMillis() - start);
    }

    private void closeOldPackages() {
        filePackagerService.closeOldPackages();
    }

    @Scheduled(initialDelayString = "${regards.file.packaging.schedule.initial.delay.ms:"
                                    + DEFAULT_INITIAL_DELAY_IN_MS
                                    + "}",
               fixedDelayString = "${regards.file.packaging.schedule.delay.ms:" + DEFAULT_SCHEDULING_DELAY_IN_MS + "}")
    private void scheduleFilePackaging() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, LOCK_ACTIONS);
                lockingTaskExecutors.executeWithLock(filePackagingTask,
                                                     new LockConfiguration(Instant.now(),
                                                                           LOCK_ID,
                                                                           Duration.ofSeconds(lockDurationInSeconds),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(LOCK_ACTIONS, LOCK_TITLE, e);
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
