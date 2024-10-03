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
package fr.cnes.regards.modules.ltamanager.service.submission.deletion;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * @author Iliana Ghazali
 **/
@Service
@Profile("!noscheduler")
@EnableScheduling
public class SubmissionDeleteScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionDeleteScheduler.class);

    // CONSTANTS

    static final String DELETE_EXPIRED_REQUESTS = "DELETE SUBMISSION REQUESTS EXPIRED";

    public static final String DEFAULT_INITIAL_DELAY = "5";       // 5min

    public static final String DEFAULT_SCHEDULING_DELAY = "1440"; // 1440min -> 24h

    public static final String DELETE_EXPIRED_REQ_SCHEDULER_LOCK = "delete-expired-submission-scheduler-lock";

    //  SERVICES

    private final SubmissionDeleteExpiredService deleteExpiredService;

    private final ITenantResolver tenantResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ILockingTaskExecutors lockingTaskExecutors;

    public SubmissionDeleteScheduler(SubmissionDeleteExpiredService deleteExpiredService,
                                     ITenantResolver tenantResolver,
                                     IRuntimeTenantResolver runtimeTenantResolver,
                                     ILockingTaskExecutors lockingTaskExecutors) {
        this.deleteExpiredService = deleteExpiredService;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.lockingTaskExecutors = lockingTaskExecutors;
    }

    @Scheduled(initialDelayString = "${regards.ltamanager.schedule.deletion.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY
                                    + "}",
               fixedDelayString = "${regards.ltamanager.schedule.deletion.delay:" + DEFAULT_SCHEDULING_DELAY + "}",
               timeUnit = TimeUnit.MINUTES)
    public void scheduleUpdateRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, DELETE_EXPIRED_REQUESTS);
                lockingTaskExecutors.executeWithLock(new DeleteTask(),
                                                     new LockConfiguration(Instant.now(),
                                                                           DELETE_EXPIRED_REQ_SCHEDULER_LOCK,
                                                                           Duration.ofSeconds(120),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(DELETE_EXPIRED_REQUESTS, DELETE_EXPIRED_REQUESTS, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    class DeleteTask implements LockingTaskExecutor.TaskWithResult<JobInfo> {

        @Override
        public JobInfo call() {
            lockingTaskExecutors.assertLocked();
            return deleteExpiredService.scheduleJob();
        }
    }
}
