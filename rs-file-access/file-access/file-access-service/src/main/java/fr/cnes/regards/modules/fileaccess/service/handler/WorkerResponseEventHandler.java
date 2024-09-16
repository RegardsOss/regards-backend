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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.fileaccess.service.handler;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageWorkerRequestEvent;
import fr.cnes.regards.modules.fileaccess.service.FileStorageService;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Event handler for worker response.
 *
 * @author tguillou
 */
@Component
public class WorkerResponseEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<ResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerResponseEventHandler.class);

    private static final String STORAGE_WORKER_EVENT_CONTENT_TYPE_PATTERN =
        FilesStorageRequestReadyToProcessEventHandler.CONTENT_TYPE_HEADER_VALUE
        + ".*";

    private final ISubscriber subscriber;

    private final FileStorageService fileStorageService;

    public WorkerResponseEventHandler(ISubscriber subscriber, FileStorageService fileStorageService) {
        this.subscriber = subscriber;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(ResponseEvent.class, this);
    }

    @Override
    public Errors validate(ResponseEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<ResponseEvent> messages) {
        LOGGER.debug("[STORE REQUEST EVENT HANDLER] Handling {} ResponseEvent...", messages.size());
        long start = System.currentTimeMillis();

        List<ResponseEvent> storageWorkerRequestEvent = keepOnlyStorageWorkerResponseEvent(messages);
        fileStorageService.filterWorkerResponse(storageWorkerRequestEvent);

        LOGGER.info("[WORKER RESPONSE EVENT HANDLER] {} ResponseEvent received", messages.size());

        LOGGER.debug("[WORKER RESPONSE EVENT HANDLER] {} ResponseEvent handled in {} ms",
                     messages.size(),
                     System.currentTimeMillis() - start);
    }

    private List<ResponseEvent> keepOnlyStorageWorkerResponseEvent(List<ResponseEvent> messages) {
        return messages.stream().filter(message -> {
            String header = message.getMessageProperties().getHeader(StorageWorkerRequestEvent.CONTENT_TYPE_HEADER);
            return header != null && header.matches(STORAGE_WORKER_EVENT_CONTENT_TYPE_PATTERN);
        }).toList();

    }
}
