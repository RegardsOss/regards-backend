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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.notification.dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * A service to authorize or not notification read requests in database.
 * Read is forbidden if number of stored notifications in database is too high.
 *
 * @author tguillou
 */
@Service
public class NotificationReadAuthorizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationReadAuthorizer.class);

    @Value("${regards.notification.cache.duration.hours:2 }") // 2 hours by default
    private int CACHE_EXPIRATION_DURATION_IN_HOURS = 2;

    @Value("${regards.notification.stored.max.allowed:1000000}") // a million by default
    private int MAX_NOTIFICATION_ALLOWED = 1000000;

    /**
     * Cache of number of notification in t_notification table, per tenant.
     * Key is the tenant, and value is the number of notification
     */
    private final Cache<String, Long> countCache;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final INotificationRepository notificationRepository;

    public NotificationReadAuthorizer(IRuntimeTenantResolver runtimeTenantResolver,
                                      INotificationRepository notificationRepository) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.notificationRepository = notificationRepository;
        this.countCache = Caffeine.newBuilder()
                                  .expireAfterWrite(CACHE_EXPIRATION_DURATION_IN_HOURS, TimeUnit.HOURS)
                                  .build();
    }

    /**
     * Control number of notifications in t_notification table. If number is too high, all read requests to t_notification are forbidden.
     *
     * @throws ModuleException if number of notifications in database is too high. This behaviour is to avoid huge and long requests to database.
     *                         When it occurs, an admin needs to manually removes notifications in database and then restart rs-admin to clear cache.
     */
    public void checkAuthorization() throws ModuleException {
        String currentTenant = runtimeTenantResolver.getTenant();
        Long mostRecentNotificationCount = countCache.getIfPresent(currentTenant);
        if (mostRecentNotificationCount == null) {
            mostRecentNotificationCount = notificationRepository.count();
            this.countCache.put(currentTenant, mostRecentNotificationCount);
        }
        if (mostRecentNotificationCount > MAX_NOTIFICATION_ALLOWED) {
            LOGGER.error("""
                             Cannot read notifications in database : too many rows (%s count / %s max allowed)
                             Please clean following datatable :
                             - t_notification
                             - ta_notification_projectuser_email
                             - ta_notification_role_name
                             """.formatted(mostRecentNotificationCount, MAX_NOTIFICATION_ALLOWED));
            throw new ModuleException("Cannot read notifications : too much entries. Contact an administrator to "
                                      + "clean the database");
        }
    }

    /**
     * Force clear cache, only for test use
     * Another way to clean cache is to reboot service.
     */
    public void clearCache() {
        this.countCache.invalidateAll();
    }
}
