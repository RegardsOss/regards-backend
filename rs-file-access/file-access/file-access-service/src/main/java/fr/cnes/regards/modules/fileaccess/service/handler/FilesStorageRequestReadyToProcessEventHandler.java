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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageResponseEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageWorkerRequestEvent;
import fr.cnes.regards.modules.fileaccess.dto.AbstractStoragePluginConfigurationDto;
import fr.cnes.regards.modules.fileaccess.dto.output.StorageResponseErrorEnum;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IStorageLocation;
import fr.cnes.regards.modules.fileaccess.service.StoragePluginConfigurationService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.*;

/**
 * Event Handler for Storage Requests received from the file catalog.
 * For each valid received request, a {@link StorageWorkerRequestEvent} will be sent to the worker manager.
 *
 * @author Thibaud Michaudel
 **/
@Component
public class FilesStorageRequestReadyToProcessEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FileStorageRequestReadyToProcessEvent> {

    public static final String CONTENT_TYPE_HEADER_VALUE = "store-";

    private final ISubscriber subscriber;

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

    public FilesStorageRequestReadyToProcessEventHandler(ISubscriber subscriber,
                                                         StoragePluginConfigurationService storagePluginConfigurationService,
                                                         IPublisher publisher,
                                                         IRuntimeTenantResolver runtimeTenantResolver,
                                                         IPluginService pluginService) {
        this.subscriber = subscriber;
        this.storagePluginConfigurationService = storagePluginConfigurationService;
        this.publisher = publisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.pluginService = pluginService;
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

        List<StorageWorkerRequestEvent> workerEventsToSend = new ArrayList<>();
        List<StorageResponseEvent> resultEventsToSend = new ArrayList<>();
        Map<String, Optional<AbstractStoragePluginConfigurationDto>> configurations = new HashMap<>();
        for (FileStorageRequestReadyToProcessEvent message : messages) {
            Optional<AbstractStoragePluginConfigurationDto> oConfiguration = configurations.computeIfAbsent(message.getStorage(),
                                                                                                            storagePluginConfigurationService::getByName);
            if (oConfiguration.isEmpty()) {
                String errorMessage = String.format(
                    "Error while processing storage request for file %s. No configuration found for %s",
                    message.getChecksum(),
                    message.getStorage());
                LOGGER.error(errorMessage);
                resultEventsToSend.add(StorageResponseEvent.createErrorResponse(message.getRequestId(),
                                                                                message.getOriginUrl(),
                                                                                message.getChecksum(),
                                                                                StorageResponseErrorEnum.UNKNOWN_STORAGE_LOCATION,
                                                                                errorMessage));

            } else {

                if (message.isReference()) {
                    // This is a reference request (no physical storage will be done, the file just need to be
                    // validated).
                    resultEventsToSend.add(validateReferenceUrl(message));
                } else {
                    // This is a physical storage request (the worker will handle the storage)
                    workerEventsToSend.add(createWorkerEvent(message, oConfiguration.get()));
                }
            }
        }
        if (!workerEventsToSend.isEmpty()) {
            publisher.publish(workerEventsToSend);
        }
        if (!resultEventsToSend.isEmpty()) {
            publisher.publish(resultEventsToSend);
        }
        LOGGER.info("[STORE REQUEST EVENT HANDLER] {} File Storage Request received", messages.size());

        LOGGER.debug("[STORAGE REQUEST EVENT HANDLER] {} FileReferenceEvent handled in {} ms",
                     messages.size(),
                     System.currentTimeMillis() - start);
    }

    private StorageWorkerRequestEvent createWorkerEvent(FileStorageRequestReadyToProcessEvent message,
                                                        AbstractStoragePluginConfigurationDto configuration) {
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
                                                                              message.getSubDirectory() != null ?
                                                                                  message.getSubDirectory() :
                                                                                  null,
                                                                              needToComputeImageSize,
                                                                              message.isActivateSmallFilePackaging(),
                                                                              configuration);
        // Headers
        eventToSend.setHeader(StorageWorkerRequestEvent.CONTENT_TYPE_HEADER,
                              CONTENT_TYPE_HEADER_VALUE + message.getStorage());
        eventToSend.setHeader(StorageWorkerRequestEvent.REQUEST_ID_HEADER, message.getRequestId());
        eventToSend.setHeader(StorageWorkerRequestEvent.TENANT_HEADER, runtimeTenantResolver.getTenant());
        eventToSend.setHeader(StorageWorkerRequestEvent.OWNER_HEADER, message.getOwner());
        eventToSend.setHeader(StorageWorkerRequestEvent.SESSION_HEADER, message.getSession());
        return eventToSend;
    }

    private StorageResponseEvent validateReferenceUrl(FileStorageRequestReadyToProcessEvent request) {
        try {
            IStorageLocation storagePlugin = pluginService.getPlugin(request.getStorage());
            Set<String> errors = Sets.newHashSet();
            if (storagePlugin.isValidUrl(request.getOriginUrl(), errors)) {
                return StorageResponseEvent.createSuccessResponse(request.getRequestId(),
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
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            return StorageResponseEvent.createErrorResponse(request.getRequestId(),
                                                            request.getOriginUrl(),
                                                            request.getChecksum(),
                                                            StorageResponseErrorEnum.INVALID_REQUEST_CONTENT,
                                                            String.format("The file reference url %s reference the "
                                                                          + "storage %s which does no exist or is not"
                                                                          + " active",
                                                                          request.getOriginUrl(),
                                                                          request.getStorage()));
        }
    }

}
