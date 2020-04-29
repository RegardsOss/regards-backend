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
package fr.cnes.regards.modules.notifier.task;

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
import fr.cnes.regards.modules.notifier.service.INotificationRuleService;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;

/**
 * Enable feature task scheduling
 *
 * @author Kevin Marchois
 *
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class NotificationTaskScheduler extends AbstractTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTaskScheduler.class);

    private static final String NOTIFICATION_LOCK = "notification";

    private static final String NOTIFICATION_TITLE = "Notification scheduling";

    private static final String NOTIFICATION_ACTIONS = "NOTIFICATION ACTIONS";

    private static final String DEFAULT_INITIAL_DELAY = "30000";

    private static final String DEFAULT_SCHEDULING_DELAY = "3000";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private INotificationRuleService notificationService;

    @Autowired
    private LockingTaskExecutors lockingTaskExecutors;

    private final Task notification_task = () -> {
        LockAssert.assertLocked();
        long start = System.currentTimeMillis();
        int nb = this.notificationService.scheduleRequests();
        if (nb != 0) {
            LOGGER.info("{} notification request(s) scheduled in {} ms", nb, System.currentTimeMillis() - start);
        }
    };

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Scheduled(
            initialDelayString = "${regards.notification.request.scheduling.initial.delay:" + DEFAULT_INITIAL_DELAY
                    + "}",
            fixedDelayString = "${regards.notification.request.scheduling.delay:" + DEFAULT_SCHEDULING_DELAY + "}")
    public void scheduleUpdateRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, NOTIFICATION_ACTIONS);
                lockingTaskExecutors
                        .executeWithLock(notification_task,
                                         new LockConfiguration(NOTIFICATION_LOCK, Instant.now().plusSeconds(60)));
            } catch (Throwable e) {
                handleSchedulingError(NOTIFICATION_ACTIONS, NOTIFICATION_TITLE, e);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
