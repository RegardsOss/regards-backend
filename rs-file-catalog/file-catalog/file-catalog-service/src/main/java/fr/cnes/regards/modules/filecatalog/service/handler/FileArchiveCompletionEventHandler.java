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
import fr.cnes.regards.modules.file.packager.amqp.FileArchiveCompletionEvent;
import fr.cnes.regards.modules.file.packager.dto.FileArchiveCompletionDto;
import fr.cnes.regards.modules.filecatalog.service.FileReferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for messages sent by the file packager when an archive has been successfully stored. One message is sent for
 * each file contained in the archive. The corresponding FileReference is then updated to inform that the file is now
 * fully {@link fr.cnes.regards.modules.fileaccess.dto.FileArchiveStatus#STORED STORED}
 *
 * @author Thibaud Michaudel
 **/
@Component
public class FileArchiveCompletionEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FileArchiveCompletionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileArchiveCompletionEventHandler.class);

    private final ISubscriber subscriber;

    private final FileReferenceService fileReferenceService;

    @Value("${regards.file.catalog.files.archive.completion.bulk.size:1000}")
    private int bulkSize;

    public FileArchiveCompletionEventHandler(ISubscriber subscriber, FileReferenceService fileReferenceService) {
        this.subscriber = subscriber;
        this.fileReferenceService = fileReferenceService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FileArchiveCompletionEvent.class, this);
    }

    @Override
    public void handleBatch(List<FileArchiveCompletionEvent> messages) {
        LOGGER.debug("[FileArchiveCompletionEvent HANDLER] Received {} archive completion responses from file packager",
                     messages.size());
        long start = System.currentTimeMillis();

        // For each different storages, call the update method with the checksum of FileReferences that need to be set
        // to STORED
        messages.stream()
                .collect(Collectors.groupingBy(FileArchiveCompletionDto::getStorage,
                                               Collectors.mapping(FileArchiveCompletionDto::getChecksum,
                                                                  Collectors.toList())))
                .forEach(fileReferenceService::updateFileReferenceStored);

        LOGGER.info("[FileArchiveCompletionEvent EVENT] {} archive completion responses handled in {} ms",
                    messages.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Errors validate(FileArchiveCompletionEvent message) {
        return null;
    }

    @Override
    public boolean isRetryEnabled() {
        return true;
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return true;
    }
}
