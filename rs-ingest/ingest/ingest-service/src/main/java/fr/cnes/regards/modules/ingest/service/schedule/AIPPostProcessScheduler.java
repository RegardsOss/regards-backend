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
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import fr.cnes.regards.modules.ingest.service.aip.AIPPostProcessService;
import static fr.cnes.regards.modules.ingest.service.schedule.SchedulerConstant.*;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;

/**
 * Scheduler to handle created {@link AIPPostProcessRequest}
 *
 * @author Iliana Ghazali
 */
@Profile("!noscheduler")
@Component
public class AIPPostProcessScheduler extends AbstractTaskScheduler {

    public static final Logger LOGGER = LoggerFactory.getLogger(AIPPostProcessScheduler.class);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private AIPPostProcessService aipPostProcessService;

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    /**
     * Post process task
     */
    private final Task postProcessTask = () -> {
        LockAssert.assertLocked();
        aipPostProcessService.scheduleJob();
    };

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(initialDelayString = DEFAULT_INITIAL_DELAY,
            fixedDelayString = "${regards.ingest.aip.post-process.bulk.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    protected void scheduleAIPPostProcessingJobs() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, POST_PROCESS_REQUESTS);
                lockingTaskExecutors.executeWithLock(postProcessTask, new LockConfiguration(POST_PROCESS_REQUEST_LOCK,
                                                                                            Instant.now().plusSeconds(
                                                                                                    MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(POST_PROCESS_REQUESTS, POST_PROCESS_TITLE, e);
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
