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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.modules.session.manager.service.handlers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SessionStepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler for new {@link SessionStepEvent}s
 *
 * @author Iliana Ghazali
 **/
public class SessionManagerHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<SessionStepEvent> {

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private SessionManagerHandlerService sessionManagerHandlerService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(SessionStepEvent.class, this);
    }

    @Override
    public Errors validate(SessionStepEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<SessionStepEvent> messages) {
        LOGGER.trace("[SESSION STEP EVENT HANDLER] Handling {} SessionStepEvents...", messages.size());
        long start = System.currentTimeMillis();
        sessionManagerHandlerService.createSessionSteps(messages);
        LOGGER.trace("[SESSION STEP EVENT HANDLER] {} SessionStepEvents handled in {} ms",
                     messages.size(),
                     System.currentTimeMillis() - start);
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return false;
    }
}
