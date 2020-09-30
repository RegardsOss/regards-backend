/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.schedule;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.service.aip.AIPDeletionService;
import fr.cnes.regards.modules.ingest.service.job.OAISDeletionsCreatorJob;
import static fr.cnes.regards.modules.ingest.service.schedule.SchedulerConstant.*;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;

/**
 * Scheduler to handle created {@link OAISDeletionRequest}s.<br/>
 * {@link OAISDeletionRequest}s are created by the {@link OAISDeletionsCreatorJob}.
 *
 * @author Sébastien Binda
 *
 */
@Profile("!noschedule")
@Component
public class OAISDeletionJobScheduler extends AbstractTaskScheduler {

    public static final Logger LOGGER = LoggerFactory.getLogger(OAISDeletionJobScheduler.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private AIPDeletionService aipDeletionService;

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    /**
     * OAIS Deletion Task
     */
    private final Task aipDeletionTask = () -> {
        LockAssert.assertLocked();
        aipDeletionService.scheduleJob();
    };

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(initialDelayString = DEFAULT_INITIAL_DELAY,
            fixedDelayString = "${regards.ingest.aip.delete.bulk.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    protected void scheduleOAISDeletionJobs() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, AIP_DELETION_REQUESTS);
                lockingTaskExecutors.executeWithLock(aipDeletionTask, new LockConfiguration(AIP_DELETION_REQUEST_LOCK,
                                                                                            Instant.now().plusSeconds(
                                                                                                    MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(AIP_DELETION_REQUESTS, AIP_DELETION_TITLE, e);
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
