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
package fr.cnes.regards.modules.notifier.task;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.notifier.service.INotificationRuleService;
import fr.cnes.regards.modules.notifier.service.IRecipientService;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;

/**
 * Enable feature task scheduling
 *
 * @author Kevin Marchois
 * @author Sylvain Vissiere-Guerinet
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class NotifierTaskScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierTaskScheduler.class);

    private static final String NOTIFICATION_LOCK = "notification";

    private static final String NOTIFICATION_MATCHING_LOCK = "notification_matching";

    private static final String NOTIFICATION_TITLE = "Notification scheduling";

    private static final String NOTIFICATION_ACTIONS = "NOTIFICATION ACTIONS";

    private static final String NOTIFICATION_CHECK_SUCCESS = "NOTIFICATION CHECK SUCCESS";

    private static final String DEFAULT_INITIAL_DELAY = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY = "3000";

    private static final String NOTIFICATION_MATCHING = "NOTIFICATION MATCHING";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private INotificationRuleService notificationService;

    private final Task notificationMatchingTask = () -> {
        LockAssert.assertLocked();
        long start = System.currentTimeMillis();
        Pair<Integer, Integer> nbNotifNbRecipient = notificationService.matchRequestNRecipient();
        if (nbNotifNbRecipient != null && nbNotifNbRecipient.getFirst() != 0) {
            LOGGER.info("{} notification requests matched to {} recipients  scheduled in {} ms",
                        nbNotifNbRecipient.getFirst(),
                        nbNotifNbRecipient.getSecond(),
                        System.currentTimeMillis() - start);
        }
    };

    @Autowired
    private IRecipientService recipientService;

    private final Task notificationTask = () -> {
        LockAssert.assertLocked();
        long start = System.currentTimeMillis();
        int nb = this.recipientService.scheduleNotificationJobs();
        if (nb != 0) {
            LOGGER.info("{} notification requests scheduled in {} ms", nb, System.currentTimeMillis() - start);
        }
    };

    private final Task notificationCheckSuccessTask = () -> {
        LockAssert.assertLocked();
        long start = System.currentTimeMillis();
        int nb = this.notificationService.checkSuccess();
        if (nb != 0) {
            LOGGER.info("{} notification request success have been detected in {} ms", nb, System.currentTimeMillis() - start);
        }
    };

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Scheduled(initialDelayString = "${regards.notification.request.scheduling.initial.delay:" + DEFAULT_INITIAL_DELAY
            + "}", fixedDelayString = "${regards.notification.request.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleMatchingRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, NOTIFICATION_MATCHING);
                lockingTaskExecutors.executeWithLock(notificationMatchingTask,
                                                     new LockConfiguration(NOTIFICATION_MATCHING_LOCK,
                                                                           Instant.now().plusSeconds(60)));
            } catch (Throwable e) {
                handleSchedulingError(NOTIFICATION_MATCHING, NOTIFICATION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(initialDelayString = "${regards.notification.request.scheduling.initial.delay:" + DEFAULT_INITIAL_DELAY
            + "}",
            fixedDelayString = "${regards.notification.request.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleNotificationJobs() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, NOTIFICATION_ACTIONS);
                lockingTaskExecutors.executeWithLock(notificationTask,
                                                     new LockConfiguration(NOTIFICATION_LOCK,
                                                                           Instant.now().plusSeconds(60)));
            } catch (Throwable e) {
                handleSchedulingError(NOTIFICATION_ACTIONS, NOTIFICATION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Scheduled(initialDelayString = "${regards.notification.request.scheduling.initial.delay:" + DEFAULT_INITIAL_DELAY
            + "}",
            fixedDelayString = "${regards.notification.request.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleNotificationRequestCheckSuccess() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, NOTIFICATION_CHECK_SUCCESS);
                lockingTaskExecutors.executeWithLock(notificationCheckSuccessTask,
                                                     new LockConfiguration(NOTIFICATION_LOCK,
                                                                           Instant.now().plusSeconds(60)));
            } catch (Throwable e) {
                handleSchedulingError(NOTIFICATION_ACTIONS, NOTIFICATION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
