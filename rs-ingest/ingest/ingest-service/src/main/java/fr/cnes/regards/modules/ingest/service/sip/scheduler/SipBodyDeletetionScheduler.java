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
package fr.cnes.regards.modules.ingest.service.sip.scheduler;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.service.job.SIPBodyDeletionJob;
import fr.cnes.regards.modules.ingest.service.schedule.SchedulerConstant;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler to handle created {@link SIPBodyDeletionRequestScheduler}s.<br/>
 * Deletions are done by the {@link SIPBodyDeletionJob}.
 *
 * @author tguillou
 */
@Profile("!noscheduler")
@Component
public class SipBodyDeletetionScheduler extends AbstractTaskScheduler {

    public static final Logger LOGGER = LoggerFactory.getLogger(SipBodyDeletetionScheduler.class);

    private static final String DEFAULT_DELAY_BEFORE_FIRST_EXECUTION_IN_HOURS = "1";

    private static final String DEFAULT_DELAY_BETWEEN_EACH_EXEC_IN_HOURS = "168"; // 24h x 7 day = 168

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    @Autowired
    private SIPBodyDeletionRequestScheduler sipBodyDeletionRequestScheduler;

    /**
     * SIP Deletion Task
     */
    private final Task sipDeletionTask = () -> {
        lockingTaskExecutors.assertLocked();
        sipBodyDeletionRequestScheduler.scheduleJob();
    };

    @Scheduled(initialDelayString = "${regards.ingest.schedule.sip.auto-deletion.initial.delay:"
                                    + DEFAULT_DELAY_BEFORE_FIRST_EXECUTION_IN_HOURS
                                    + "}",
               fixedDelayString = "${regards.ingest.schedule.sip.auto-deletion.delay:"
                                  + DEFAULT_DELAY_BETWEEN_EACH_EXEC_IN_HOURS
                                  + "}",
               timeUnit = TimeUnit.HOURS)
    public void scheduleSIPBodyDeletionJob() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, SchedulerConstant.SIP_BODY_DELETION_REQUESTS);
                lockingTaskExecutors.executeWithLock(sipDeletionTask,
                                                     new LockConfiguration(SchedulerConstant.SIP_BODY_DELETION_REQUEST_LOCK,
                                                                           Instant.now()
                                                                                  .plusSeconds(SchedulerConstant.MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(SchedulerConstant.SIP_BODY_DELETION_REQUESTS,
                                      SchedulerConstant.SIP_BODY_DELETION_TITLE,
                                      e);
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
