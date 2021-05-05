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
package fr.cnes.regards.framework.modules.session.management.service.handlers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Handler for new {@link SessionStepEvent}s
 *
 * @author Iliana Ghazali
 **/
@Component
@Profile("!nohandler")
public class SessionManagerHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<SessionStepEvent> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private SessionManagerHandlerService sessionManagerHandlerService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(SessionStepEvent.class, this);
    }

    @Override
    public boolean validate(String tenant, SessionStepEvent message) {
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<SessionStepEvent> messages) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.trace("[SESSION STEP EVENT HANDLER] Handling {} SessionStepEvents...", messages.size());
            long start = System.currentTimeMillis();
            sessionManagerHandlerService.createSessionSteps(messages);
            LOGGER.trace("[SESSION STEP EVENT HANDLER] {} SessionStepEvents handled in {} ms", messages.size(),
                        System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }


}
