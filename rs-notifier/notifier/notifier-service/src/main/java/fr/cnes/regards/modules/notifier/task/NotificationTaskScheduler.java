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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.modules.locks.service.ILockService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.notifier.service.INotificationRuleService;

/**
 * Enable feature task scheduling
 *
 * @author Kevin Marchois
 *
 */
@Component
@Profile("!noscheduler")
@EnableScheduling
public class NotificationTaskScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTaskScheduler.class);

    private static final String LOCK_NOTIFICATION_REQUEST = "Notification_Request";

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ILockService lockService;

    @Autowired
    private INotificationRuleService notificationService;

    @Scheduled(initialDelayString = "${regards.notification.request.scheduling.initial.delay:30000}",
            fixedDelayString = "${regards.notification.request.scheduling.delay:1000}")
    public void scheduleUpdateRequests() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                LOGGER.trace("LOCKING FOR TENANT {} IN SCHEDULE NOTIFICATION REQUESTS", tenant);
                if (lockService.obtainLockOrSkip(LOCK_NOTIFICATION_REQUEST, this, 60)) {
                    LOGGER.trace("LOCK OBTAINED FOR TENANT {} IN SCHEDULE NOTIFICATION REQUESTS", tenant);
                    try {
                        long start = System.currentTimeMillis();
                        int nb = this.notificationService.scheduleRequests();
                        if (nb != 0) {
                            LOGGER.info("{} update request(s) scheduled in {} ms", nb,
                                        System.currentTimeMillis() - start);
                        }
                    } finally {
                        LOGGER.trace("RELEASING OBTAINED LOCK FOR TENANT {} IN SCHEDULE NOTIFICATION REQUESTS", tenant);
                        lockService.releaseLock(LOCK_NOTIFICATION_REQUEST, this);
                        LOGGER.trace("LOCK RELEASED FOR TENANT {} IN SCHEDULE NOTIFICATION REQUESTS", tenant);
                    }
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

}
