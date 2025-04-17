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
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageResponseEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageWorkerRequestEvent;
import fr.cnes.regards.modules.fileaccess.dto.output.StorageResponseErrorEnum;
import fr.cnes.regards.modules.fileaccess.dto.output.worker.StorageWorkerResponseDto;
import fr.cnes.regards.modules.fileaccess.dto.output.worker.type.ImageFileMetadata;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import fr.cnes.regards.modules.workermanager.amqp.events.in.RequestEvent;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Service to manage file storage (using workers) in file-access
 *
 * @author Thibaud Michaudel
 **/
@Service
public class FileStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);

    private final IPublisher publisher;

    private final StoragePluginConfigurationService storagePluginConfigurationService;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IPluginService pluginService;

    /**
     * File datatypes for which image size calculation is needed
     */
    private final List<DataType> imageTypes = List.of(DataType.QUICKLOOK_SD,
                                                      DataType.QUICKLOOK_MD,
                                                      DataType.QUICKLOOK_HD,
                                                      DataType.THUMBNAIL);

    public FileStorageService(IPublisher publisher,
                              StoragePluginConfigurationService storagePluginConfigurationService,
                              IRuntimeTenantResolver runtimeTenantResolver,
                              IPluginService pluginService) {
        this.publisher = publisher;
        this.storagePluginConfigurationService = storagePluginConfigurationService;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.pluginService = pluginService;
    }

    /**
     * Create a {@link StorageResponseEvent} from the given {@link ResponseEvent} when
     */
    public void filterWorkerResponse(List<ResponseEvent> storageWorkerRequestEvent) {
        for (ResponseEvent message : storageWorkerRequestEvent) {
            switch (message.getState()) {
                case SKIPPED -> {
                    LOGGER.error("Worker is not active");
                    publisher.publish(StorageResponseEvent.createSimpleErrorResponse(message.getRequestId(),
                                                                                     StorageResponseErrorEnum.INACTIVE_WORKER,
                                                                                     "Worker is not active"),
                                      message.getRequestId());
                }
                case GRANTED -> {
                    // ignore response
                }
                case DELAYED -> {
                    LOGGER.warn("{} request is delayed", message.getRequestId());
                }
                case INVALID_CONTENT -> {
                    publisher.publish(StorageResponseEvent.createSimpleErrorResponse(message.getRequestId(),
                                                                                     StorageResponseErrorEnum.INVALID_REQUEST_CONTENT,
                                                                                     "Invalid request content"),
                                      message.getRequestId());
                }
                case ERROR -> {
                    Collection<String> messageList = message.getMessage();
                    String messagesJoined = messageList != null ? String.join("\n", messageList) : "Error";
                    publisher.publish(StorageResponseEvent.createSimpleErrorResponse(message.getRequestId(),
                                                                                     StorageResponseErrorEnum.WORKER_ERROR,
                                                                                     messagesJoined),
                                      message.getRequestId());
                }
                case SUCCESS -> {
                    StorageWorkerResponseDto workerResponseContent = extractWorkerResponse(message);
                    if (workerResponseContent == null) {
                        publisher.publish(StorageResponseEvent.createSimpleErrorResponse(message.getRequestId(),

                                                                                         StorageResponseErrorEnum.WORKER_RESPONSE_EMPTY,
                                                                                         "Worker response is "
                                                                                         + "null"),
                                          message.getRequestId());
                    } else {
                        Integer height = null;
                        Integer width = null;
                        if (workerResponseContent.getStoreFileMetadata() instanceof ImageFileMetadata imageFileMetadata) {
                            height = imageFileMetadata.getHeightInPx();
                            width = imageFileMetadata.getWidthInPx();
                        }
                        if (isStoredInCache(workerResponseContent)) {
                            publisher.publish(StorageResponseEvent.createSuccessCacheResponse(message.getRequestId(),
                                                                                              workerResponseContent.getStoreFileMetadata()
                                                                                                                   .getStoredFileUrl(),
                                                                                              workerResponseContent.getStoreFileMetadata()
                                                                                                                   .getChecksum(),
                                                                                              workerResponseContent.getStoreFileMetadata()
                                                                                                                   .getFileSizeInBytes(),
                                                                                              height,
                                                                                              width,
                                                                                              workerResponseContent.getFileProcessingMetadata()
                                                                                                                   .getStoreParentUrl(),
                                                                                              workerResponseContent.getFileProcessingMetadata()
                                                                                                                   .getCachePath()),
                                              message.getRequestId());
                        } else {
                            publisher.publish(StorageResponseEvent.createSuccessResponse(message.getRequestId(),
                                                                                         workerResponseContent.getStoreFileMetadata()
                                                                                                              .getStoredFileUrl(),
                                                                                         workerResponseContent.getStoreFileMetadata()
                                                                                                              .getChecksum(),
                                                                                         workerResponseContent.getStoreFileMetadata()
                                                                                                              .getFileSizeInBytes(),
                                                                                         height,
                                                                                         width),
                                              message.getRequestId());
                        }

                    }
                }
            }
        }
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

    private boolean isStoredInCache(StorageWorkerResponseDto workerResponseContent) {
        return workerResponseContent.getFileProcessingMetadata().getCachePath() != null;
    }

    /**
     * For reference requests, validate the reference url and send a response to the sender.
     * For storage requests, dispatch the requests to the storage worker.
     */
    public void processStorageRequests(List<FileStorageRequestReadyToProcessEvent> messages) {
        List<StorageWorkerRequestEvent> workerEventsToSend = new ArrayList<>();
        Map<String, Optional<StoragePluginConfigurationDtoAndPluginId>> configurations = new HashMap<>();
        Map<String, Optional<IStorageLocation>> storageLocations = new HashMap<>();
        for (FileStorageRequestReadyToProcessEvent message : messages) {
            if (message.isReference()) {
                // This is a reference request (no physical storage will be done, the file
                // just need to be validated).
                Optional<IStorageLocation> oStorageLocation = storageLocations.computeIfAbsent(message.getStorage(),
                                                                                               this::getPluginIfExists);
                publisher.publish(validateReferenceUrl(message, oStorageLocation), message.getRequestId());
            } else {
                Optional<StoragePluginConfigurationDtoAndPluginId> oConfiguration = configurations.computeIfAbsent(
                    message.getStorage(),
                    storagePluginConfigurationService::getByName);
                if (oConfiguration.isEmpty()) {
                    String errorMessage = String.format(
                        "Error while processing storage request for file %s. No configuration found for %s",
                        message.getChecksum(),
                        message.getStorage());
                    LOGGER.error(errorMessage);
                    publisher.publish(StorageResponseEvent.createErrorResponse(message.getRequestId(),
                                                                               message.getOriginUrl(),
                                                                               message.getChecksum(),
                                                                               StorageResponseErrorEnum.UNKNOWN_STORAGE_LOCATION,
                                                                               errorMessage), message.getRequestId());

                } else {
                    // This is a physical storage request (the worker will handle the storage)
                    workerEventsToSend.add(createWorkerEvent(message, oConfiguration.get()));
                }
            }

        }
        if (!workerEventsToSend.isEmpty()) {
            publisher.publish(workerEventsToSend,
                              "regards.broadcast." + RequestEvent.class.getName(),
                              Optional.empty());
        }
    }

    private StorageWorkerRequestEvent createWorkerEvent(FileStorageRequestReadyToProcessEvent message,
                                                        StoragePluginConfigurationDtoAndPluginId configuration) {
        // Body
        boolean needToComputeImageSize = MediaType.parseMediaType(message.getMetadata().getMimeType())
                                                  .getType()
                                                  .equals("image")
                                         && imageTypes.contains(DataType.valueOf(message.getMetadata().getType()))
                                         && (message.getMetadata().getHeight() == 0
                                             || message.getMetadata().getWidth() == 0);

        StorageWorkerRequestEvent eventToSend = new StorageWorkerRequestEvent(message.getChecksum(),
                                                                              message.getAlgorithm(),
                                                                              message.getOriginUrl(),
                                                                              message.getSubDirectory(),
                                                                              needToComputeImageSize,
                                                                              message.isActivateSmallFilePackaging(),
                                                                              configuration.storagePluginConfigurationDto());
        // Headers
        eventToSend.setHeader(StorageWorkerRequestEvent.CONTENT_TYPE_HEADER,
                              FileAccessConstants.CONTENT_TYPE_HEADER + configuration.pluginId());
        eventToSend.setHeader(StorageWorkerRequestEvent.REQUEST_ID_HEADER, message.getRequestId());
        eventToSend.setHeader(StorageWorkerRequestEvent.TENANT_HEADER, runtimeTenantResolver.getTenant());
        eventToSend.setHeader(StorageWorkerRequestEvent.OWNER_HEADER, message.getOwner());
        eventToSend.setHeader(StorageWorkerRequestEvent.SESSION_HEADER, message.getSession());
        return eventToSend;
    }

    private StorageResponseEvent validateReferenceUrl(FileStorageRequestReadyToProcessEvent request,
                                                      Optional<IStorageLocation> storageLocation) {
        Set<String> errors = Sets.newHashSet();
        if (storageLocation.isPresent()) {
            if (storageLocation.get().isValidUrl(request.getOriginUrl(), errors)) {
                return StorageResponseEvent.createSuccessReferenceResponse(request.getRequestId(),
                                                                           request.getOriginUrl(),
                                                                           request.getChecksum());
            } else {
                return StorageResponseEvent.createErrorResponse(request.getRequestId(),
                                                                request.getOriginUrl(),
                                                                request.getChecksum(),
                                                                StorageResponseErrorEnum.INVALID_REQUEST_CONTENT,
                                                                String.format("The file reference url %s format is not"
                                                                              + " valid for storage location %s. "
                                                                              + "Cause : %s",
                                                                              request.getOriginUrl(),
                                                                              request.getStorage(),
                                                                              errors));
            }
        } else {
            // The request reference a virtual Storage Location, no validation required
            return StorageResponseEvent.createSuccessReferenceResponse(request.getRequestId(),
                                                                       request.getOriginUrl(),
                                                                       request.getChecksum());
        }
    }

    private Optional<IStorageLocation> getPluginIfExists(String storageName) {
        try {
            return pluginService.getPlugin(storageName);
        } catch (ModuleException e) {
            return Optional.empty();
        }
    }
}
