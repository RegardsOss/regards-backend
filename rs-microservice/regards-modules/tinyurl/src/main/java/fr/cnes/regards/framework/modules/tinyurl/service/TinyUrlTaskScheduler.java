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
package fr.cnes.regards.framework.modules.tinyurl.service;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Enable tiny URL task scheduling
 *
 * @author Marc SORDI
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class TinyUrlTaskScheduler extends AbstractTaskScheduler {

    public static final Long MAX_TASK_DELAY = 60L; // In second

    private static final Logger LOGGER = LoggerFactory.getLogger(TinyUrlTaskScheduler.class);

    private static final String DEFAULT_INITIAL_DELAY = "60000";

    private static final String DEFAULT_SCHEDULING_DELAY = "3600000";

    private static final String PURGE_TINYURL_LOCK = "purgeTinyUrl";

    private static final String PURGE_TINYURL = "TINY URL PURGE";

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private TinyUrlService tinyUrlService;

    private final Task purgeTask = () -> {
        LockAssert.assertLocked();
        tinyUrlService.purge();
    };

    @Scheduled(initialDelayString = "${regards.framework.modules.tinyurl.scheduling.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY
                                    + "}",
               fixedDelayString = "${regards.framework.modules.tinyurl.scheduling.delay:"
                                  + DEFAULT_SCHEDULING_DELAY
                                  + "}")
    public void purgeTinyUrls() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, PURGE_TINYURL);
                lockingTaskExecutors.executeWithLock(purgeTask,
                                                     new LockConfiguration(PURGE_TINYURL_LOCK,
                                                                           Instant.now().plusSeconds(MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(PURGE_TINYURL, PURGE_TINYURL, e);
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
