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
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageResponseEvent;
import fr.cnes.regards.modules.fileaccess.dto.output.StorageResponseDto;
import fr.cnes.regards.modules.filecatalog.service.FileStorageRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler for event sent when a file has been stored by file access. The attribute
 * {@link StorageResponseEvent#isStoredInCache() isStoredInCache} indicate if the file has been fully stored or if it
 * has been stored in a local cache and further processing is needed. If it is, an event will is sent to the file
 * packager so the file is packaged in an archive that will be stored later by file access.
 *
 * @author Thibaud Michaudel
 **/
public class StorageResponseEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<StorageResponseEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageResponseEventHandler.class);

    private final ISubscriber subscriber;

    private final FileStorageRequestService fileStorageRequestService;

    @Value("${regards.file.catalog.storage.response.event.bulk.size:1000}")
    private int bulkSize;

    public StorageResponseEventHandler(ISubscriber subscriber, FileStorageRequestService fileStorageRequestService) {
        this.subscriber = subscriber;
        this.fileStorageRequestService = fileStorageRequestService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(StorageResponseEvent.class, this);
    }

    @Override
    public void handleBatch(List<StorageResponseEvent> messages) {
        LOGGER.debug("[StorageResponseEvent HANDLER] {} storage response events received", messages.size());
        long start = System.currentTimeMillis();

        // Separate successes and errors
        Map<Boolean, List<StorageResponseEvent>> responseBySuccessStatus = messages.stream()
                                                                                   .collect(Collectors.groupingBy(
                                                                                       StorageResponseDto::isRequestSuccessful));
        // Handle successes
        if (!responseBySuccessStatus.get(Boolean.TRUE).isEmpty()) {
            fileStorageRequestService.processFileStorageSuccessResponses(responseBySuccessStatus.get(Boolean.TRUE));
        }
        // Handle errors
        if (!responseBySuccessStatus.get(Boolean.FALSE).isEmpty()) {
            fileStorageRequestService.processFileStorageErrorResponses(responseBySuccessStatus.get(Boolean.FALSE));
        }

        LOGGER.info("[StorageResponseEvent HANDLER] {} storage response events handled in {} ms",
                    messages.size(),
                    System.currentTimeMillis() - start);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Errors validate(StorageResponseEvent message) {
        return null;
    }

    @Override
    public boolean isRetryEnabled() {
        return true;
    }
}