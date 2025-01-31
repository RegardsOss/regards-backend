/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduler to periodically delete files whose package is STORED.
 *
 * @author Thibaud Michaudel
 **/
@Component
@Profile("!noscheduler")
@EnableScheduling
public class FileDeletingScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDeletingScheduler.class);

    private static final String DEFAULT_INITIAL_DELAY_IN_MS = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY_IN_MS = "3600000";

    private static final String LOCK_ID = "file-packager-file-deleting";

    private static final String LOCK_TITLE = "File Packager file deleting scheduling";

    private static final String LOCK_ACTIONS = "FILE PACKAGER DELETE FILES ACTIONS";

    private final LockingTaskExecutor.Task fileDeletingTask;

    private final ILockingTaskExecutors lockingTaskExecutors;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ITenantResolver tenantResolver;

    private final FilePackagerService filePackagerService;

    @Value("${regards.file.packager.file.deleting.lock.duration:60}")
    private int lockDuration;

    public FileDeletingScheduler(ILockingTaskExecutors lockingTaskExecutors,
                                 IRuntimeTenantResolver runtimeTenantResolver,
                                 ITenantResolver tenantResolver,
                                 FilePackagerService filePackagerService) {
        fileDeletingTask = () -> {
            lockingTaskExecutors.assertLocked();
            filePackagerService.scheduleDeleteLocalFilesJobs();
        };
        this.lockingTaskExecutors = lockingTaskExecutors;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
        this.filePackagerService = filePackagerService;
    }

    @Scheduled(initialDelayString = "${regards.file.packager.schedule.initial.delay.ms:"
                                    + DEFAULT_INITIAL_DELAY_IN_MS
                                    + "}",
               fixedDelayString = "${regards.file.packager.schedule.delay.ms:" + DEFAULT_SCHEDULING_DELAY_IN_MS + "}")
    private void scheduleDeleteFile() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, LOCK_ACTIONS);
                lockingTaskExecutors.executeWithLock(fileDeletingTask,
                                                     new LockConfiguration(Instant.now(),
                                                                           LOCK_ID,
                                                                           Duration.ofSeconds(lockDuration),
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