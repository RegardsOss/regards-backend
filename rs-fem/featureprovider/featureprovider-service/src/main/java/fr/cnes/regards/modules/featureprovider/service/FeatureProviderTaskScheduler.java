/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.featureprovider.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;

/**
 * Enable feature task scheduling
 *
 * @author Marc SORDI
 *
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class FeatureProviderTaskScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderTaskScheduler.class);

    private static final Long MAX_TASK_DELAY = 60L; // In second

    private static final String NOTIFICATION_TITLE = "Feature scheduling";

    private static final String EXTRACTION_REQUEST_LOCK = "scheduleFExtraction";

    private static final String EXTRACTION_REQUESTS = "FEATURE EXTRACTION REQUESTS";

    private static final String DEFAULT_INITIAL_DELAY = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY = "3000";

    private static final String LOG_FORMAT = "[{}] {} {} scheduled in {} ms";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IFeatureExtractionService featureExtractionService;

    private final Task extractionTask = () -> {
        LockAssert.assertLocked();
        long start = System.currentTimeMillis();
        int nb = this.featureExtractionService.scheduleRequests();
        if (nb != 0) {
            LOGGER.info(LOG_FORMAT, INSTANCE_RANDOM_ID, nb, EXTRACTION_REQUESTS, System.currentTimeMillis() - start);
        }
    };

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
            fixedDelayString = "${regards.feature.request.reference.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleExtractionRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, EXTRACTION_REQUESTS);
                lockingTaskExecutors.executeWithLock(extractionTask,
                                                     new LockConfiguration(EXTRACTION_REQUEST_LOCK,
                                                                           Instant.now().plusSeconds(MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(EXTRACTION_REQUESTS, NOTIFICATION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }
}
