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
package fr.cnes.regards.framework.modules.session.commons.service.delete;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionDeleteEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Handle deletion of a session
 *
 * @author Iliana Ghazali
 **/
public class SessionDeleteEventHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<SessionDeleteEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionDeleteEventHandler.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private List<ISessionDeleteService> sessionDeleteServices;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(SessionDeleteEvent.class, this);
    }

    @Override
    public void handle(String tenant, SessionDeleteEvent message) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            String source = message.getSource();
            String session = message.getSession();
            long start = System.currentTimeMillis();

            LOGGER.trace("Handling deleting of session {} from source {} for tenant {}", source, session, tenant);
            for (ISessionDeleteService sessionDeleteService : sessionDeleteServices) {
                sessionDeleteService.deleteSession(message.getSource(), message.getSession());
            }
            LOGGER.trace("Deleting of session {} from source {} for tenant {} handled in {}ms", source, session, tenant,
                         start - System.currentTimeMillis());
        } finally {
            runtimeTenantResolver.clearTenant();

        }
    }
}