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
package fr.cnes.regards.modules.ingest.service.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;
import fr.cnes.regards.modules.ingest.domain.flow.InputSipFlow;
import fr.cnes.regards.modules.ingest.service.IIngestService;

/**
 * Manage input/output SIP flow
 *
 * @author Marc SORDI
 *
 */
@Component
public class SIPFlowHandler implements ApplicationListener<ApplicationReadyEvent>, IHandler<InputSipFlow> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPFlowHandler.class);

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(InputSipFlow.class, this);
    }

    @Override
    public void handle(TenantWrapper<InputSipFlow> wrapper) {
        LOGGER.trace("New SIP data flow event detected for tenant {}", wrapper.getTenant());
        InputSipFlow event = wrapper.getContent();

        // Handle data flow for specified tenant
        try {
            // Set working tenant
            runtimeTenantResolver.forceTenant(wrapper.getTenant());
            ingestService.validateIngestMetadata(event.getMetadata());
            SIPDto dto = ingestService.store(event.getSip(), event.getMetadata());
            // FIXME do something
        } catch (EntityInvalidException e) {
            // Invalid ingest metadata
            // FIXME do something
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

}
