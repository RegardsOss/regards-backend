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
package fr.cnes.regards.modules.crawler.service.scheduler;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.crawler.domain.EntityEventRequest;
import fr.cnes.regards.modules.crawler.service.service.IEntityIndexerService;
import fr.cnes.regards.modules.crawler.service.job.UpdateEntityIntoEsJob;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduler to schedule {@link UpdateEntityIntoEsJob}s that will update the ES index following a dataset update
 *
 * @author Thibaud Michaudel
 **/
@Component
@EnableScheduling
@Profile({ "!noscheduler" })
public class SaveEntityIntoEsScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveEntityIntoEsScheduler.class);

    private static final String DEFAULT_INITIAL_DELAY_IN_MS = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY_IN_MS = "1000";

    private static final String LOCK_ID = "Save Entity Lock";

    private static final String LOCK_TITLE = "Save Entity Into ES Scheduler";

    private static final String LOCK_ACTIONS = "Saving entities";

    private final LockingTaskExecutor.Task saveEntityIntoEsTask;

    private final ILockingTaskExecutors lockingTaskExecutors;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final ITenantResolver tenantResolver;

    private final IEntityIndexerService entityIndexerService;

    @Value("${regards.dam.save.entity.into.es.lock.duration:60}")
    private int lockDuration;

    @Value("${regards.dam.page.size:100}")
    private int pageSize;

    public SaveEntityIntoEsScheduler(ILockingTaskExecutors lockingTaskExecutors,
                                     IRuntimeTenantResolver runtimeTenantResolver,
                                     ITenantResolver tenantResolver,
                                     IEntityIndexerService entityIndexerService) {
        saveEntityIntoEsTask = () -> {
            lockingTaskExecutors.assertLocked();
            saveEntityIntoEs();
        };
        this.lockingTaskExecutors = lockingTaskExecutors;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.tenantResolver = tenantResolver;
        this.entityIndexerService = entityIndexerService;
    }

    public void saveEntityIntoEs() {
        long start = System.currentTimeMillis();
        LOGGER.trace("[SAVE ENTITY INTO ES SCHEDULER] Checking files waiting for packaging ...");

        Pageable pageable = PageRequest.of(0, pageSize);
        Page<EntityEventRequest> page;
        do {
            page = entityIndexerService.scheduleUpdateEntityIntoEsJob(pageable);
            pageable = pageable.next();
        } while (page.hasContent());

        LOGGER.trace("[SAVE ENTITY INTO ES SCHEDULER] Requests packaged in {} ms", System.currentTimeMillis() - start);
    }

    @Scheduled(initialDelayString = "${regards.dam.schedule.initial.delay.ms:" + DEFAULT_INITIAL_DELAY_IN_MS + "}",
               fixedDelayString = "${regards.dam.schedule.delay.ms:" + DEFAULT_SCHEDULING_DELAY_IN_MS + "}")
    public void scheduleSaveEntityIntoEs() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, LOCK_ACTIONS);
                lockingTaskExecutors.executeWithLock(saveEntityIntoEsTask,
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