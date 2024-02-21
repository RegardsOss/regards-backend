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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageResponseEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageWorkerRequestEvent;
import fr.cnes.regards.modules.fileaccess.dto.AbstractStoragePluginConfigurationDto;
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

    public static final String UNKNOWN_STORAGE_LOCATION = "UNKNOWN_STORAGE_LOCATION";

    private final ISubscriber subscriber;

    private final IPublisher publisher;

    private final StoragePluginConfigurationService storagePluginConfigurationService;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final List<DataType> imageType = List.of(DataType.QUICKLOOK_SD,
                                                     DataType.QUICKLOOK_MD,
                                                     DataType.QUICKLOOK_HD,
                                                     DataType.THUMBNAIL);

    public FilesStorageRequestReadyToProcessEventHandler(ISubscriber subscriber,
                                                         StoragePluginConfigurationService storagePluginConfigurationService,
                                                         IPublisher publisher,
                                                         IRuntimeTenantResolver runtimeTenantResolver) {
        this.subscriber = subscriber;
        this.storagePluginConfigurationService = storagePluginConfigurationService;
        this.publisher = publisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
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

        List<StorageWorkerRequestEvent> eventsToSend = new ArrayList<>();
        List<StorageResponseEvent> errorsToSend = new ArrayList<>();
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
                errorsToSend.add(StorageResponseEvent.createErrorResponse(message.getRequestId(),
                                                                          message.getOriginUrl(),
                                                                          message.getChecksum(),
                                                                          UNKNOWN_STORAGE_LOCATION,
                                                                          errorMessage));

            } else {
                // Body
                boolean needToComputeImageSize = MediaType.parseMediaType(message.getMetadata().getMimeType())
                                                          .getType()
                                                          .equals("image") && imageType.contains(DataType.valueOf(
                    message.getMetadata().getType())) && (message.getMetadata().getHeight() == 0
                                                          || message.getMetadata().getWidth() == 0);

                StorageWorkerRequestEvent eventToSend = createEventToSend(message,
                                                                          needToComputeImageSize,
                                                                          oConfiguration);

                eventsToSend.add(eventToSend);
            }
        }
        publisher.publish(eventsToSend);
        publisher.publish(errorsToSend);
        LOGGER.info("[STORE REQUEST EVENT HANDLER] {} File Storage Request received", messages.size());

        LOGGER.debug("[STORAGE REQUEST EVENT HANDLER] {} FileReferenceEvent handled in {} ms",
                     messages.size(),
                     System.currentTimeMillis() - start);
    }

    private StorageWorkerRequestEvent createEventToSend(FileStorageRequestReadyToProcessEvent message,
                                                        boolean needToComputeImageSize,
                                                        Optional<AbstractStoragePluginConfigurationDto> oConfiguration) {
        //FIXME: fix activateSmallFilePackaging value according to the request origin
        StorageWorkerRequestEvent eventToSend = new StorageWorkerRequestEvent(message.getChecksum(),
                                                                              message.getAlgorithm(),
                                                                              message.getOriginUrl(),
                                                                              message.getSubDirectory() != null ?
                                                                                  message.getSubDirectory() :
                                                                                  null,
                                                                              needToComputeImageSize,
                                                                              true,
                                                                              oConfiguration.get());
        // Headers
        eventToSend.setHeader(StorageWorkerRequestEvent.CONTENT_TYPE_HEADER, "store-" + message.getStorage());
        eventToSend.setHeader(StorageWorkerRequestEvent.REQUEST_ID_HEADER, message.getRequestId());
        eventToSend.setHeader(StorageWorkerRequestEvent.TENANT_HEADER, runtimeTenantResolver.getTenant());
        eventToSend.setHeader(StorageWorkerRequestEvent.OWNER_HEADER, message.getOwner());
        eventToSend.setHeader(StorageWorkerRequestEvent.SESSION_HEADER, message.getSession());
        return eventToSend;
    }

}
