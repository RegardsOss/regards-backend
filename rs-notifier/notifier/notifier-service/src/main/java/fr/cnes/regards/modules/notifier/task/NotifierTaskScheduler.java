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
package fr.cnes.regards.modules.notifier.task;

import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.notifier.service.IRecipientService;
import fr.cnes.regards.modules.notifier.service.NotificationMatchingService;
import fr.cnes.regards.modules.notifier.service.NotificationProcessingService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

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

    private static final String NOTIFICATION_CHECK_COMPLETED = "NOTIFICATION CHECK COMPLETED";

    private static final String DEFAULT_INITIAL_DELAY = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY = "3000";

    private static final String NOTIFICATION_MATCHING = "NOTIFICATION MATCHING";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private NotificationProcessingService notificationProcessingService;

    @Autowired
    private NotificationMatchingService notificationMatchingService;

    @Autowired
    private IRecipientService recipientService;

    @Autowired
    private ILockingTaskExecutors lockingTaskExecutors;

    private final Task notificationMatchingTask = () -> {

        lockingTaskExecutors.assertLocked();
        long start = System.currentTimeMillis();

        Pair<Integer, Integer> nbNotifNbRecipient = notificationMatchingService.matchRequestNRecipient();

        if (nbNotifNbRecipient != null && nbNotifNbRecipient.getFirst() != 0) {
            LOGGER.info("{} notification requests matched to {} recipients. Scheduled in {} ms.",
                        nbNotifNbRecipient.getFirst(),
                        nbNotifNbRecipient.getSecond(),
                        System.currentTimeMillis() - start);
        }
    };

    private final Task notificationTask = () -> {

        lockingTaskExecutors.assertLocked();
        long start = System.currentTimeMillis();

        int nb = recipientService.scheduleNotificationJobs();

        if (nb != 0) {
            LOGGER.info("{} notification requests scheduled in {} ms", nb, System.currentTimeMillis() - start);
        }
    };

    private final Task notificationCheckCompletedTask = () -> {

        lockingTaskExecutors.assertLocked();
        long start = System.currentTimeMillis();

        Pair<Integer, Integer> result = notificationProcessingService.checkCompletedRequests();

        long stop = System.currentTimeMillis() - start;
        int total = result.getFirst() + result.getSecond();
        if (total != 0) {
            LOGGER.info("{} completed notification requests have been detected in {} ms. Successes : {} - Errors : {}",
                        total,
                        stop,
                        result.getFirst(),
                        result.getSecond());
        }
    };

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Scheduled(initialDelayString = "${regards.notification.request.scheduling.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY
                                    + "}",
               fixedDelayString = "${regards.notification.request.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleMatchingRequests() {

        schedule(NOTIFICATION_MATCHING, notificationMatchingTask, NOTIFICATION_MATCHING_LOCK);

    }

    @Scheduled(initialDelayString = "${regards.notification.request.scheduling.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY
                                    + "}",
               fixedDelayString = "${regards.notification.request.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleNotificationJobs() {

        schedule(NOTIFICATION_ACTIONS, notificationTask, NOTIFICATION_LOCK);

    }

    @Scheduled(initialDelayString = "${regards.notification.request.scheduling.initial.delay:"
                                    + DEFAULT_INITIAL_DELAY
                                    + "}",
               fixedDelayString = "${regards.notification.request.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleNotificationRequestCheckCompleted() {

        schedule(NOTIFICATION_CHECK_COMPLETED, notificationCheckCompletedTask, NOTIFICATION_LOCK);

    }

    private void schedule(String type, Task task, String lock) {

        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, type);
                lockingTaskExecutors.executeWithLock(task,
                                                     new LockConfiguration(Instant.now(),
                                                                           lock,
                                                                           Duration.ofSeconds(300),
                                                                           Duration.ZERO));
            } catch (Throwable e) {
                handleSchedulingError(type, NOTIFICATION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
