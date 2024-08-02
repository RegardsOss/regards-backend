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
package fr.cnes.regards.modules.feature.service.request;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationRequest;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduler to handle {@link FeatureUpdateDisseminationRequest} using {@link FeatureUpdateDisseminationService}
 *
 * @author Léo Mieulet
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class FeatureUpdateDisseminationScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureUpdateDisseminationScheduler.class);

    private static final String NOTIFICATION_TITLE = "Feature update dissemination scheduling";

    private static final String DEFAULT_INITIAL_DELAY = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY = "2000";

    private static final String LOG_FORMAT = "[{}] {} {} scheduled in {} ms";

    private static final String UPDATE_FEATURE_DISSEMINATION_LOCK = "scheduleFeatureDisseminationUpdate";

    private static final String UPDATE_REQUESTS = "FEATURE UPDATE DISSEMINATION REQUESTS";

    private static final long MAX_TASK_DELAY = 180;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private FeatureUpdateDisseminationService featureUpdateDisseminationService;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    private final LockingTaskExecutor.Task updateFeatureDisseminationTask = () -> {
        lockingTaskExecutors.assertLocked();
        long start = System.currentTimeMillis();
        int nb = featureUpdateDisseminationService.handleRequests();
        if (nb != 0) {
            LOGGER.info(LOG_FORMAT, INSTANCE_RANDOM_ID, nb, UPDATE_REQUESTS, System.currentTimeMillis() - start);
        }
    };

    @Scheduled(initialDelayString = "${regards.feature.update.dissemination.request.scheduling.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY
                                    + "}",
               fixedDelayString = "${regards.feature.update.dissemination.request.scheduling.delay:"
                                  + DEFAULT_SCHEDULING_DELAY
                                  + "}")
    public void scheduleUpdateRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, UPDATE_REQUESTS);
                lockingTaskExecutors.executeWithLock(updateFeatureDisseminationTask,
                                                     new LockConfiguration(Instant.now(),
                                                                           UPDATE_FEATURE_DISSEMINATION_LOCK,
                                                                           Duration.ofSeconds(MAX_TASK_DELAY),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(UPDATE_REQUESTS, NOTIFICATION_TITLE, e);
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
