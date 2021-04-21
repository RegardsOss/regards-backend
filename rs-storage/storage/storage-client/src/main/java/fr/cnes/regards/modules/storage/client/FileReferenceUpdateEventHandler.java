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
package fr.cnes.regards.modules.storage.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceUpdateEvent;
import sun.misc.Request;

/**
 * Handle bus messages {@link FileReferenceUpdateEvent}
 * @author Sébastien Binda
 */
@Component("clientRequestUpdateEventHandler")
public class FileReferenceUpdateEventHandler
        implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FileReferenceUpdateEvent> {

    @Autowired(required = false)
    private IStorageFileListener listener;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (listener != null) {
            subscriber.subscribeTo(FileReferenceUpdateEvent.class, this);
        } else {
            LOGGER.warn("No listener configured to collect storage FileReferenceUpdateEvent bus messages !!");
        }
    }

    @Override
    public boolean validate(String tenant, FileReferenceUpdateEvent message) {
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<FileReferenceUpdateEvent> messages) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.debug("[STORAGE RESPONSES HANDLER] Handling {} FileReferenceUpdateEventHandler...", messages.size());
            long start = System.currentTimeMillis();
            handle(messages);
            LOGGER.debug("[STORAGE RESPONSES HANDLER] {} FileReferenceUpdateEventHandler handled in {} ms", messages.size(),
                         System.currentTimeMillis() - start);
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    private void handle(List<FileReferenceUpdateEvent> events) {
        List<FileReferenceUpdateDTO> dtos = new ArrayList<>();
        for(FileReferenceUpdateEvent event: events) {
            dtos.add(new FileReferenceUpdateDTO(event));
        }
        if(!dtos.isEmpty()) {
            listener.onFileUpdated(dtos);
        }
    }

}
