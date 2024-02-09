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
package fr.cnes.regards.modules.filecatalog.service.handler;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.filecatalog.service.FileStorageRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler to handle {@link FilesStorageRequestEvent} AMQP messages.<br>
 * Those messages are sent to create new file reference.<br>
 * Each message is saved in a concurrent list to handle availability request by bulk.
 *
 * @author Thibaud Michaudel
 */
@Component
public class FilesStorageRequestEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FilesStorageRequestEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesStorageRequestEventHandler.class);

    private final ISubscriber subscriber;

    private final FileStorageRequestService fileStorageRequestService;

    public FilesStorageRequestEventHandler(ISubscriber subscriber,
                                           FileStorageRequestService fileStorageRequestService) {
        this.subscriber = subscriber;
        this.fileStorageRequestService = fileStorageRequestService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FilesStorageRequestEvent.class, this);
    }

    @Override
    public void handleBatch(List<FilesStorageRequestEvent> messages) {
        LOGGER.debug("[FILE STORAGE REQUEST EVENT HANDLER] Bulk saving {} FilesStorageRequestEvent...",
                     messages.size());
        long start = System.currentTimeMillis();
        fileStorageRequestService.createStorageRequests(messages);
        LOGGER.info("[FILE STORAGE REQUEST EVENT HANDLER] {} FilesStorageRequestEvent handled in {} ms",
                    messages.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public Errors validate(FilesStorageRequestEvent message) {
        return null;
    }
    
}
