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
package fr.cnes.regards.modules.ingest.service.session;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionRequestDto;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionSelectionMode;
import fr.cnes.regards.modules.ingest.service.IIngestService;
import fr.cnes.regards.modules.sessionmanager.domain.event.DeleteSessionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Handler to remove SIP related to a session
 * @author Léo Mieulet
 */
@Component
public class DeleteSessionEventHandler implements ApplicationListener<ApplicationReadyEvent>, IHandler<DeleteSessionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteSessionEventHandler.class);

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(DeleteSessionEvent.class, this);
    }

    @Override
    public void handle(TenantWrapper<DeleteSessionEvent> wrapper) {
        DeleteSessionEvent event = wrapper.getContent();
        LOGGER.debug("Event receive to program the deletion of all SIP from session {} {}", event.getSource(),
                event.getName());
        // Set working tenant
        runtimeTenantResolver.forceTenant(wrapper.getTenant());
        // Run a SessionDeletionJob
        ingestService.registerSessionDeletionRequest(SessionDeletionRequestDto.build(
                event.getSource(), event.getName(),
                SessionDeletionMode.IRREVOCABLY, SessionDeletionSelectionMode.INCLUDE
        ));
    }

}
