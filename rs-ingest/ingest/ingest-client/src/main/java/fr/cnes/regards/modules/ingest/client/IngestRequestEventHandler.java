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
package fr.cnes.regards.modules.ingest.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;

/**
 *
 * Listen to {@link IngestRequestEvent} and call back the client on each one.
 *
 * @author Marc SORDI
 */
@Component
public class IngestRequestEventHandler
        implements IHandler<IngestRequestEvent>, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestRequestEventHandler.class);

    @Autowired(required = false)
    private IIngestClientListener listener;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (listener != null) {
            subscriber.subscribeTo(IngestRequestEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect ingest request events!");
        }

    }

    @Override
    public void handle(TenantWrapper<IngestRequestEvent> wrapper) {
        String tenant = wrapper.getTenant();
        IngestRequestEvent event = wrapper.getContent();
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.trace("Handling {}", event.toString());
            RequestInfo info = RequestInfo.build(event.getRequestId(), event.getProviderId(), event.getSipId(),
                                                 event.getErrors());
            switch (event.getState()) {
                case DENIED:
                    listener.onDenied(info);
                    break;
                case GRANTED:
                    listener.onGranted(info);
                    break;
                case ERROR:
                    listener.onError(info);
                    break;
                case SUCCESS:
                    listener.onSuccess(info);
                    break;
                default:
                    break;
            }
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }
}
