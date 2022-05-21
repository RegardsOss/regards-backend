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
package fr.cnes.regards.modules.notification.service;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.notification.domain.NotificationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * This handler absorbs the incoming notification events
 *
 * @author Marc SORDI
 */
@Component
public class NotificationEventHandler
    implements IHandler<NotificationEvent>, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventHandler.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private IInstanceSubscriber instanceSubscriber;

    @Autowired
    private ISubscriber subscriber;

    @Value("${regards.notification.mode:MULTITENANT}")
    private NotificationMode notificationMode;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (notificationMode == NotificationMode.INSTANCE) {
            instanceSubscriber.subscribeTo(NotificationEvent.class, this);
        } else {
            subscriber.subscribeTo(NotificationEvent.class, this);
        }
    }

    @Override
    public void handle(TenantWrapper<NotificationEvent> wrapper) {
        LOGGER.trace("New notification event for tenant {}", wrapper.getTenant());
        NotificationEvent notification = wrapper.getContent();
        //Either we are in multitenant mode and we have to force tenant or we aren't
        if (notificationMode == NotificationMode.MULTITENANT) {
            try {
                // Set working tenant
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                FeignSecurityManager.asSystem();
                notificationService.createNotification(notification.getNotification());
            } finally {
                FeignSecurityManager.reset();
                runtimeTenantResolver.clearTenant();
            }
        } else {
            notificationService.createNotification(notification.getNotification());
        }
    }
}
