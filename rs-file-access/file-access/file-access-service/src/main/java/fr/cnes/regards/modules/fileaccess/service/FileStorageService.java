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
package fr.cnes.regards.modules.fileaccess.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageResponseEvent;
import fr.cnes.regards.modules.fileaccess.dto.output.StorageResponseErrorEnum;
import fr.cnes.regards.modules.fileaccess.dto.output.worker.StorageWorkerResponseDto;
import fr.cnes.regards.modules.fileaccess.dto.output.worker.type.ImageFileMetadata;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Service to manage file storage (using workers) in file-access
 *
 * @author Thibaud Michaudel
 **/
@Service
@MultitenantTransactional
public class FileStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);

    private final IPublisher publisher;

    public FileStorageService(IPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * Create a {@link StorageResponseEvent} from the given {@link ResponseEvent} when
     */
    public void filterWorkerResponse(List<ResponseEvent> storageWorkerRequestEvent) {
        List<StorageResponseEvent> eventsToSend = new ArrayList<>();
        for (ResponseEvent message : storageWorkerRequestEvent) {
            Long requestId = Long.valueOf(message.getRequestId());
            StorageResponseEvent storageResponseEvent;
            switch (message.getState()) {
                case SKIPPED -> {
                    LOGGER.error("Worker is not active");
                    eventsToSend.add(StorageResponseEvent.createSimpleErrorResponse(requestId,
                                                                                    StorageResponseErrorEnum.INACTIVE_WORKER,
                                                                                    "Worker is not active"));
                }
                case GRANTED, DELAYED -> {
                    // ignore response
                }
                case INVALID_CONTENT -> {
                    eventsToSend.add(StorageResponseEvent.createSimpleErrorResponse(requestId,
                                                                                    StorageResponseErrorEnum.INVALID_REQUEST_CONTENT,
                                                                                    "Invalid request content"));
                }
                case ERROR -> {
                    Collection<String> messageList = message.getMessage();
                    String messagesJoined = messageList != null ? String.join("\n", messageList) : "Error";
                    eventsToSend.add(StorageResponseEvent.createSimpleErrorResponse(requestId,
                                                                                    StorageResponseErrorEnum.WORKER_ERROR,
                                                                                    messagesJoined));
                }
                case SUCCESS -> {
                    StorageWorkerResponseDto workerResponseContent = extractWorkerResponse(message);
                    if (workerResponseContent == null) {
                        storageResponseEvent = StorageResponseEvent.createSimpleErrorResponse(requestId,

                                                                                              StorageResponseErrorEnum.WORKER_RESPONSE_EMPTY,
                                                                                              "Worker response is null");
                    } else {
                        Integer height = null;
                        Integer weight = null;
                        if (workerResponseContent.getStoreFileMetadata() instanceof ImageFileMetadata imageFileMetadata) {
                            height = imageFileMetadata.getHeightInPx();
                            weight = imageFileMetadata.getWidthInPx();
                        }
                        storageResponseEvent = StorageResponseEvent.createSuccessResponse(requestId,
                                                                                          workerResponseContent.getStoreFileMetadata()
                                                                                                               .getStoredFileUrl(),
                                                                                          workerResponseContent.getStoreFileMetadata()
                                                                                                               .getChecksum(),
                                                                                          workerResponseContent.getStoreFileMetadata()
                                                                                                               .getFileSizeInBytes(),
                                                                                          height,
                                                                                          weight,
                                                                                          computeIsStoredInCached(
                                                                                              workerResponseContent));
                    }
                    eventsToSend.add(storageResponseEvent);
                }
            }
        }
        publisher.publish(eventsToSend);
    }

    private StorageWorkerResponseDto extractWorkerResponse(ResponseEvent message) {
        byte[] content = message.getContent();
        if (content == null) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(content, StorageWorkerResponseDto.class);
        } catch (IOException e) {
            throw new RsRuntimeException(e);
        }
    }

    private boolean computeIsStoredInCached(StorageWorkerResponseDto workerResponseContent) {
        return workerResponseContent.getFileProcessingMetadata().getCachePath() != null;
    }
}
