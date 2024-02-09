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
package fr.cnes.regards.modules.fileaccess.service.handler;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.fileaccess.amqp.input.FilesStorageRequestReadyToProcessEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Event Handler for Storage Requests
 *
 * @author Thibaud Michaudel
 **/
public class FilesStorageRequestReadyToProcessEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FilesStorageRequestReadyToProcessEvent> {

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FilesStorageRequestReadyToProcessEvent.class, this);
    }

    @Override
    public Errors validate(FilesStorageRequestReadyToProcessEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<FilesStorageRequestReadyToProcessEvent> messages) {
        LOGGER.debug("[STORE REQUEST EVENT HANDLER] Handling {} FilesStorageRequestEvent...", messages.size());
        long start = System.currentTimeMillis();

        // Placeholder log, the handlebatch do nothing for now as the actual process is still the responsability of
        // the old storage microservice
        LOGGER.info("[STORE REQUEST EVENT HANDLER] {} File Storage Request received", messages.size());

        LOGGER.debug("[STORAGE REQUEST EVENT HANDLER] {} FileReferenceEvent handled in {} ms",
                     messages.size(),
                     System.currentTimeMillis() - start);
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return true;
    }
}
