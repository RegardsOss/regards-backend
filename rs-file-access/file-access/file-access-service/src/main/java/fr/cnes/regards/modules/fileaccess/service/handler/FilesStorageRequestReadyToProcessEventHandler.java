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
import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageResponseEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageWorkerRequestEvent;
import fr.cnes.regards.modules.fileaccess.service.FileStorageService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Event Handler for Storage Requests received from the file catalog.
 * For each valid storage request, a {@link StorageWorkerRequestEvent} will be sent to the worker manager.
 * For each valid reference request, a success {@link StorageResponseEvent} will be sent to the origin
 * request sender (file-catalog)
 * For each error request, an error {@link StorageResponseEvent} will be sent to the origin
 * request sender (file-catalog or file-packager)
 *
 * @author Thibaud Michaudel
 **/
@Component
public class FilesStorageRequestReadyToProcessEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FileStorageRequestReadyToProcessEvent> {

    private final ISubscriber subscriber;

    private final FileStorageService fileStorageService;

    public FilesStorageRequestReadyToProcessEventHandler(ISubscriber subscriber,
                                                         FileStorageService fileStorageService) {
        this.subscriber = subscriber;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FileStorageRequestReadyToProcessEvent.class, this);
    }

    @Override
    public Errors validate(FileStorageRequestReadyToProcessEvent message) {
        return null;

    }

    @Override
    public void handleBatch(List<FileStorageRequestReadyToProcessEvent> messages) {
        LOGGER.debug("[STORE REQUEST EVENT HANDLER] Handling {} FilesStorageRequestEvent...", messages.size());
        long start = System.currentTimeMillis();

        fileStorageService.processStorageRequests(messages);

        LOGGER.info("[STORAGE REQUEST EVENT HANDLER] {} FileReferenceEvent handled in {} ms",
                    messages.size(),
                    System.currentTimeMillis() - start);
    }
}
