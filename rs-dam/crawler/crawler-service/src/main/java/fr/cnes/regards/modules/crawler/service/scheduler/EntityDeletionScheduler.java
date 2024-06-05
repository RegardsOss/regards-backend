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
import fr.cnes.regards.modules.crawler.service.EntityDeletionService;
import fr.cnes.regards.modules.crawler.service.IngesterService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduler that will periodicaly try to handle {@link fr.cnes.regards.modules.crawler.domain.EntityDeletionRequest}
 *
 * @author Thibaud Michaudel
 **/
@Component
@Profile("!noscheduler")
@EnableScheduling
public class EntityDeletionScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityDeletionScheduler.class);

    private static final String LOCK = "crawler-feature-deletion-requests";

    private static final String TITLE = "Crawler deletion requests scheduling";

    private static final String ACTION = "CRAWLER DELETION REQUESTS ACTIONS";

    private static final String DEFAULT_INITIAL_DELAY_IN_MS = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY_IN_MS = "2000";

    public static final int MAX_TASK_DELAY_IN_S = 120;

    public static final int PAGE_SIZE = 100;

    public static final int PAGE_LIMIT = 1000;

    private ITenantResolver tenantResolver;

    private IRuntimeTenantResolver runtimeTenantResolver;

    private ILockingTaskExecutors lockingTaskExecutors;

    private EntityDeletionService featureService;

    private final IngesterService ingesterService;

    private final LockingTaskExecutor.Task handleRequestsTask = () -> {
        lockingTaskExecutors.assertLocked();
        handleEntityDeletion();
    };

    public void handleEntityDeletion() {
        if (!ingesterService.lockIngestion()) {
            return;
        }
        try {
            Pageable page = PageRequest.of(0, PAGE_SIZE);
            do {
                page = featureService.handleEntityDeletion(page);
            } while (page.isPaged() && page.getPageNumber() < PAGE_LIMIT);
        } finally {
            ingesterService.releaseIngestionLock();
        }
    }

    public EntityDeletionScheduler(ITenantResolver tenantResolver,
                                   IRuntimeTenantResolver runtimeTenantResolver,
                                   ILockingTaskExecutors lockingTaskExecutors,
                                   EntityDeletionService featureService,
                                   IngesterService ingesterService) {
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.lockingTaskExecutors = lockingTaskExecutors;
        this.featureService = featureService;
        this.ingesterService = ingesterService;
    }

    @Scheduled(initialDelayString = "${regards.dam.schedule.initial.delay:" + DEFAULT_INITIAL_DELAY_IN_MS + "}",
               fixedDelayString = "${regards.dam.schedule.delay:" + DEFAULT_SCHEDULING_DELAY_IN_MS + "}")
    public void scheduleDeleteRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, ACTION);
                lockingTaskExecutors.executeWithLock(handleRequestsTask,
                                                     new LockConfiguration(Instant.now(),
                                                                           LOCK,
                                                                           Duration.ofSeconds(MAX_TASK_DELAY_IN_S),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(ACTION, TITLE, e);
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
