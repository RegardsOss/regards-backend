/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.sessionmanager.service.ISessionService;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for SessionNotificationEvent events.
 * @author Léo Mieulet
 */
@Component
@RegardsTransactional
public class SessionNotificationHandler implements IHandler<SessionMonitoringEvent>, ISessionNotificationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SessionNotificationHandler.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISessionService sessionService;

    @Autowired
    private ISubscriber subscriber;

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.storage.service.IPlop#handle(fr.cnes.regards.framework.amqp.domain.TenantWrapper)
     */
    @Override
    public void handle(TenantWrapper<SessionMonitoringEvent> wrapper) {
        String tenant = wrapper.getTenant();
        runtimeTenantResolver.forceTenant(tenant);
        LOG.debug("New SessionNotification received for {} {}", wrapper.getContent().getSource(),
                wrapper.getContent().getName());
        sessionService.updateSessionProperty(wrapper.getContent());
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.storage.service.IPlop#onApplicationEvent(org.springframework.boot.context.event.ApplicationReadyEvent)
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        // Subscribe to events on {@link StorageDataFile} changes.
        subscriber.subscribeTo(SessionMonitoringEvent.class, this);
    }
}
