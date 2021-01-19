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
package fr.cnes.regards.modules.sessionmanager.service.events;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.service.ISessionService;

/**
 * Handler for SessionNotificationEvent events.
 * @author Léo Mieulet
 */
@Component
public class SessionNotificationHandler
        implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<SessionMonitoringEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(SessionNotificationHandler.class);

    /**
     * Bulk size limit to handle messages
     */
    @Value("${regards.admin.session.notification.items.bulk.size:1000}")
    private int BULK_SIZE;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISessionService sessionService;

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(SessionMonitoringEvent.class, this);
    }

    @Override
    public boolean validate(String tenant, SessionMonitoringEvent message) {
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<SessionMonitoringEvent> messages) {
        try {
            runtimeTenantResolver.forceTenant(tenant);
            LOG.info("[SESSION NOTIFICATIONS HANDLER] Bulk saving {} notifications...", messages.size());
            long start = System.currentTimeMillis();
            sessionService.updateSessionProperties(messages);
            LOG.info("[SESSION NOTIFICATIONS HANDLER] {} Notifications handled in {} ms", messages.size(),
                     System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    @Override
    public int getBatchSize() {
        return BULK_SIZE;
    }

}
