/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service.task;

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
import fr.cnes.regards.modules.feature.service.IFeatureCopyService;
import fr.cnes.regards.modules.feature.service.IFeatureCreationService;
import fr.cnes.regards.modules.feature.service.IFeatureDeletionService;
import fr.cnes.regards.modules.feature.service.IFeatureNotificationService;
import fr.cnes.regards.modules.feature.service.IFeatureReferenceService;
import fr.cnes.regards.modules.feature.service.IFeatureUpdateService;
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
public class FeatureTaskScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTaskScheduler.class);

    private static final Long MAX_TASK_DELAY = 60L; // In second

    private static final String NOTIFICATION_TITLE = "Feature scheduling";

    private static final String CREATE_REQUEST_LOCK = "scheduleFCreate";

    private static final String CREATE_REQUESTS = "FEATURE CREATE REQUESTS";

    private static final String UPDATE_REQUEST_LOCK = "scheduleFUpate";

    private static final String UPDATE_REQUESTS = "FEATURE UPDATE REQUESTS";

    private static final String DELETE_REQUEST_LOCK = "scheduleFDelete";

    private static final String DELETE_REQUESTS = "FEATURE DELETE REQUESTS";

    private static final String COPY_REQUEST_LOCK = "scheduleFCopy";

    private static final String COPY_REQUESTS = "FEATURE COPY REQUESTS";

    private static final String REFERENCE_REQUEST_LOCK = "scheduleFReference";

    private static final String REFERENCE_REQUESTS = "FEATURE REFERENCE REQUESTS";

    private static final String NOTIFICATION_REQUEST_LOCK = "scheduleFNotification";

    private static final String NOTIFICATION_REQUESTS = "FEATURE NOTIFICATION REQUESTS";

    private static final String DEFAULT_INITIAL_DELAY = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY = "3000";

    private static final String LOG_FORMAT = "[{}] {} {} scheduled in {} ms";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IFeatureCreationService featureService;

    @Autowired
    private IFeatureUpdateService featureUpdateService;

    @Autowired
    private IFeatureDeletionService featureDeletionService;

    @Autowired
    private IFeatureReferenceService featureReferenceService;

    @Autowired
    private IFeatureCopyService featureCopyService;

    @Autowired
    private IFeatureNotificationService featureNotificationService;

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    private final Task createTask = () -> {
        LockAssert.assertLocked();
        long start = System.currentTimeMillis();
        int nb = this.featureService.scheduleRequests();
        if (nb != 0) {
            LOGGER.info(LOG_FORMAT, INSTANCE_RANDOM_ID, nb, CREATE_REQUESTS, System.currentTimeMillis() - start);
        }
    };

    private final Task updateTask = () -> {
        LockAssert.assertLocked();
        long start = System.currentTimeMillis();
        int nb = featureUpdateService.scheduleRequests();
        if (nb != 0) {
            LOGGER.info(LOG_FORMAT, INSTANCE_RANDOM_ID, nb, UPDATE_REQUESTS, System.currentTimeMillis() - start);
        }
    };

    private final Task deleteTask = () -> {
        LockAssert.assertLocked();
        long start = System.currentTimeMillis();
        int nb = featureDeletionService.scheduleRequests();
        if (nb != 0) {
            LOGGER.info(LOG_FORMAT, INSTANCE_RANDOM_ID, nb, DELETE_REQUESTS, System.currentTimeMillis() - start);
        }
    };

    private final Task referenceTask = () -> {
        LockAssert.assertLocked();
        long start = System.currentTimeMillis();
        int nb = this.featureReferenceService.scheduleRequests();
        if (nb != 0) {
            LOGGER.info(LOG_FORMAT, INSTANCE_RANDOM_ID, nb, REFERENCE_REQUESTS, System.currentTimeMillis() - start);
        }
    };

    private final Task copyTask = () -> {
        LockAssert.assertLocked();
        long start = System.currentTimeMillis();
        int nb = this.featureCopyService.scheduleRequests();
        if (nb != 0) {
            LOGGER.info(LOG_FORMAT, INSTANCE_RANDOM_ID, nb, COPY_REQUESTS, System.currentTimeMillis() - start);
        }
    };

    private final Task notificationTask = () -> {
        LockAssert.assertLocked();
        long start = System.currentTimeMillis();
        int nb = this.featureNotificationService.scheduleRequests();
        if (nb != 0) {
            LOGGER.info(LOG_FORMAT, INSTANCE_RANDOM_ID, nb, NOTIFICATION_REQUESTS, System.currentTimeMillis() - start);
        }
    };

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
            fixedDelayString = "${regards.feature.request.insert.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleInsertRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, CREATE_REQUESTS);
                lockingTaskExecutors.executeWithLock(createTask, new LockConfiguration(CREATE_REQUEST_LOCK,
                        Instant.now().plusSeconds(MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(CREATE_REQUESTS, NOTIFICATION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
            fixedDelayString = "${regards.feature.request.update.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleUpdateRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, UPDATE_REQUESTS);
                lockingTaskExecutors.executeWithLock(updateTask, new LockConfiguration(UPDATE_REQUEST_LOCK,
                        Instant.now().plusSeconds(MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(UPDATE_REQUESTS, NOTIFICATION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
            fixedDelayString = "${regards.feature.request.delete.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleDeleteRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, DELETE_REQUESTS);
                lockingTaskExecutors.executeWithLock(deleteTask, new LockConfiguration(DELETE_REQUEST_LOCK,
                        Instant.now().plusSeconds(MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(DELETE_REQUESTS, NOTIFICATION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
            fixedDelayString = "${regards.feature.request.reference.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleReferenceRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, REFERENCE_REQUESTS);
                lockingTaskExecutors.executeWithLock(referenceTask, new LockConfiguration(REFERENCE_REQUEST_LOCK,
                        Instant.now().plusSeconds(MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(REFERENCE_REQUESTS, NOTIFICATION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
            fixedDelayString = "${regards.feature.request.copy.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleCopyRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, COPY_REQUESTS);
                lockingTaskExecutors.executeWithLock(copyTask, new LockConfiguration(COPY_REQUEST_LOCK,
                        Instant.now().plusSeconds(MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(COPY_REQUESTS, NOTIFICATION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(initialDelayString = "${regards.feature.request.scheduling.initial.delay:" + DEFAULT_INITIAL_DELAY + "}",
            fixedDelayString = "${regards.feature.request.notification.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY
                    + "}")
    public void scheduleNotificationRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, NOTIFICATION_REQUESTS);
                lockingTaskExecutors.executeWithLock(notificationTask, new LockConfiguration(NOTIFICATION_REQUEST_LOCK,
                        Instant.now().plusSeconds(MAX_TASK_DELAY)));
            } catch (Throwable e) {
                handleSchedulingError(NOTIFICATION_REQUESTS, NOTIFICATION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }
}
