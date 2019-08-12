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
package fr.cnes.regards.modules.storagelight.client;

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
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestEvent;

/**
 * @author sbinda
 *
 */
@Component("clientRequestEventHandler")
public class FileRequestEventHandler implements ApplicationListener<ApplicationReadyEvent>, IHandler<FileRequestEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRequestEventHandler.class);

    @Autowired(required = false)
    private IStorageRequestListener listener;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (listener != null) {
            subscriber.subscribeTo(FileRequestEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect storage FileRequestEvent bus messages !!");
        }
    }

    @Override
    public void handle(TenantWrapper<FileRequestEvent> wrapper) {
        String tenant = wrapper.getTenant();
        FileRequestEvent event = wrapper.getContent();
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.info("Handling {}", event.toString());
            handle(event);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    private void handle(FileRequestEvent event) {
        RequestInfo info = RequestInfo.build(event.getRequestId());
        switch (event.getState()) {
            case DONE:
                handleDone(event, info);
                break;
            case ERROR:
                handleError(event, info);
                break;
            case GRANTED:
                listener.onRequestGranted(info);
                break;
            case DENIED:
                listener.onRequestDenied(info);
                break;
            default:
                break;
        }
    }

    private void handleDone(FileRequestEvent event, RequestInfo info) {
        switch (event.getType()) {
            case AVAILABILITY:
                listener.onAvailable(info);
                break;
            case DELETION:
                listener.onDeletionSuccess(info);
                break;
            case REFERENCE:
                listener.onReferenceSuccess(info);
                break;
            case STORAGE:
                listener.onStoreSuccess(info);
                break;
            case COPY:
                listener.onCopySuccess(info);
                break;
            default:
                break;
        }
    }

    private void handleError(FileRequestEvent event, RequestInfo info) {
        switch (event.getType()) {
            case AVAILABILITY:
                listener.onAvailabilityError(info, event.getErrors());
                break;
            case DELETION:
                listener.onDeletionError(info, event.getErrors());
                break;
            case REFERENCE:
                listener.onReferenceError(info, event.getErrors());
                break;
            case STORAGE:
                listener.onStoreError(info, event.getErrors());
                break;
            case COPY:
                listener.onCopyError(info, event.getErrors());
                break;
            default:
                break;
        }
    }
}
