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
 * Scheduler scheduling storage jobs for complete packages
 *
 * @author Thibaud Michaudel
 **/
@Component
@Profile("!noscheduler")
@EnableScheduling
public class CompletePackageScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompletePackageScheduler.class);

    private static final String DEFAULT_INITIAL_DELAY_IN_MS = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY_IN_MS = 6 * 60 * 60 * 1000 + ""; // 6 hours

    private static final long TRY_ACQUIRE_LOCK_TIME_IN_MS = 10 * 60 * 1000; // 10 minutes

    private static final String LOCK_ACTIONS = "FILE PACKAGER STORE COMPLETE PACKAGE ACTIONS";

    private final LockingTaskExecutor.TaskWithResult<Void> completePackageTask;

    private final ILockingTaskExecutors lockingTaskExecutors;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ITenantResolver tenantResolver;

    private final FilePackagerService filePackagerService;

    @Value("${regards.file.packager.complete.package.lock.duration.in.seconds:300}")
    private int lockDurationInSeconds;

    public CompletePackageScheduler(ILockingTaskExecutors lockingTaskExecutors,
                                    IRuntimeTenantResolver runtimeTenantResolver,
                                    ITenantResolver tenantResolver,
                                    FilePackagerService filePackagerService) {
        completePackageTask = () -> {
            lockingTaskExecutors.assertLocked();
            completePackage();
            return null;
        };
        this.lockingTaskExecutors = lockingTaskExecutors;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
        this.filePackagerService = filePackagerService;
    }

    public void completePackage() {
        filePackagerService.scheduleStoreCompletePackageJobs();
    }

    @Scheduled(initialDelayString = "${regards.file.packager.schedule.initial.delay.ms:"
                                    + DEFAULT_INITIAL_DELAY_IN_MS
                                    + "}",
               fixedDelayString = "${regards.file.packager.schedule.delay.ms:" + DEFAULT_SCHEDULING_DELAY_IN_MS + "}")
    public void scheduleCompletePackage() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, LOCK_ACTIONS);
                long start = System.currentTimeMillis();
                boolean wasExecuted = false;
                while (!wasExecuted && System.currentTimeMillis() - start < TRY_ACQUIRE_LOCK_TIME_IN_MS) {
                    wasExecuted = lockingTaskExecutors.executeWithLock(completePackageTask,
                                                                       new LockConfiguration(Instant.now(),
                                                                                             FilePackagerSchedulersLock.LOCK_ID,
                                                                                             Duration.ofSeconds(
                                                                                                 lockDurationInSeconds),
                                                                                             Duration.ZERO))
                                                      .wasExecuted();
                    Thread.sleep(50);
                }
                if (!wasExecuted) {
                    LOGGER.warn("[Complete Package Scheduler] Couldn't acquire lock after {}ms.",
                                TRY_ACQUIRE_LOCK_TIME_IN_MS);
                }
            } catch (Throwable e) {
                handleSchedulingError(LOCK_ACTIONS, FilePackagerSchedulersLock.LOCK_TITLE, e);
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