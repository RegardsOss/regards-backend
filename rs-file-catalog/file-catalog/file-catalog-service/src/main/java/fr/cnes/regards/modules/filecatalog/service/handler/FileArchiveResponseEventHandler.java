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
import fr.cnes.regards.modules.filecatalog.amqp.input.FileArchiveResponseEvent;
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
 * Handler for {@link FileArchiveResponseEvent} sent by the file packager.
 * This message is sent when a file is being successfully managed by the file packager service. The file is not yet
 * stored in the final destination storage, but is now associated with a package that will be stored eventually.
 *
 * @author Thibaud Michaudel
 **/
@Component
public class FileArchiveResponseEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FileArchiveResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileArchiveResponseEventHandler.class);

    private final ISubscriber subscriber;

    private final FileStorageRequestService fileStorageRequestService;

    /**
     * Small bulkSize because we need to get a request per message in a single db call
     */
    @Value("${regards.file.catalog.files.storage.request.bulk.size:25}")
    private int bulkSize;

    public FileArchiveResponseEventHandler(ISubscriber subscriber,
                                           FileStorageRequestService fileStorageRequestService) {
        this.subscriber = subscriber;
        this.fileStorageRequestService = fileStorageRequestService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FileArchiveResponseEvent.class, this);
    }

    @Override
    public void handleBatch(List<FileArchiveResponseEvent> messages) {
        LOGGER.debug("[FILE ARCHIVE RESPONSE EVENT HANDLER] Received {} responses for local file storage with long "
                     + "term storage pending", messages.size());
        long start = System.currentTimeMillis();
        fileStorageRequestService.handleSuccess(messages);

        LOGGER.info("[FILE ARCHIVE RESPONSE EVENT] {} files references created in status TO_STORE in {} ms",
                    messages.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Errors validate(FileArchiveResponseEvent message) {
        return null;
    }

}
