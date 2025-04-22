/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.filecatalog.service.handler;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.filecatalog.service.FileStorageRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Deprecated : This handler should be removed because storage requests and reference requests are processed the same
 * way with neo storage. The event {@link fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent}
 * should be the one used for both cases.
 *
 * @author Thibaud Michaudel
 **/
@Deprecated
@Component
public class FilesReferenceEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FilesReferenceEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesReferenceEventHandler.class);

    private final ISubscriber subscriber;

    @Value("${regards.file.catalog.files.reference.request.bulk.size:100}")
    private int bulkSize;

    private final FileStorageRequestService fileStorageRequestService;

    public FilesReferenceEventHandler(ISubscriber subscriber, FileStorageRequestService fileStorageRequestService) {
        this.subscriber = subscriber;
        this.fileStorageRequestService = fileStorageRequestService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FilesReferenceEvent.class, this);
    }

    @Override
    public void handleBatch(List<FilesReferenceEvent> messages) {
        LOGGER.debug("[FILES REFERENCE EVENT HANDLER] Bulk saving {} FilesReferenceEvent...,", messages.size());
        long start = System.currentTimeMillis();

        try {
            fileStorageRequestService.createReferenceRequests(messages);
        } catch (ModuleException e) {
            LOGGER.error("[FILES REFERENCE EVENT HANDLER] Error while handling reference requests ");
            throw new RuntimeException(e);
        }
        LOGGER.info("[FILES REFERENCE EVENT HANDLER] {} FilesReferenceEvent handled in {} ms",
                    messages.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Errors validate(FilesReferenceEvent message) {
        return null;
    }

}
