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

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;

/**
 * @author sbinda
 *
 */
// @Profile("!storageTest")
// @Component("clientFileRefEventHandler")
public class FileReferenceEventHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<FileReferenceEvent> {

    @Autowired(required = false)
    private IStorageFileListener listener;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (listener != null) {
            subscriber.subscribeTo(FileReferenceEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect storage FileReferenceEvent bus messages !!");
        }
    }

    @Override
    public void handle(TenantWrapper<FileReferenceEvent> wrapper) {
        String tenant = wrapper.getTenant();
        FileReferenceEvent event = wrapper.getContent();
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.info("Handling {}", event.toString());
            handle(event);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    private void handle(FileReferenceEvent event) {
        Set<RequestInfo> requestInfos = event.getRequestIds().stream().map(RequestInfo::build)
                .collect(Collectors.toSet());
        switch (event.getType()) {
            case AVAILABILITY_ERROR:
                listener.onFileNotAvailable(event.getChecksum(), requestInfos, event.getMessage());
                break;
            case AVAILABLE:
                listener.onFileAvailable(event.getChecksum(), requestInfos);
                break;
            case DELETED_FOR_OWNER:
                event.getOwners().forEach(o -> listener
                        .onFileDeleted(event.getChecksum(), event.getLocation().getStorage(), o, requestInfos));
                break;
            case DELETION_ERROR:
            case FULLY_DELETED:
                break;
            case STORED:
                listener.onFileStored(event.getChecksum(), event.getLocation().getStorage(), event.getOwners(),
                                      requestInfos);
                break;
            case STORE_ERROR:
                listener.onFileStoreError(event.getChecksum(), event.getLocation().getStorage(), event.getOwners(),
                                          requestInfos, event.getMessage());
                break;
            default:
                break;

        }
    }

}
