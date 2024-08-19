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
import fr.cnes.regards.modules.fileaccess.dto.StorageRequestStatus;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;
import fr.cnes.regards.modules.filecatalog.service.FileStorageRequestService;
import fr.cnes.regards.modules.filecatalog.service.scheduler.FileStorageRequestCheckScheduler;
import fr.cnes.regards.modules.filecatalog.service.scheduler.FileStorageRequestCompleteScheduler;
import fr.cnes.regards.modules.filecatalog.service.scheduler.FileStorageRequestDispatchScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler to handle {@link FilesStorageRequestEvent} AMQP messages.<br>
 * Those messages are sent to create new file reference.<br>
 * Each message is saved in a concurrent list to handle availability request by bulk. <br>
 * <br>
 * The storage request process is as follows :
 *
 * <ul>
 * <li>The {@link FilesStorageRequestEventHandler} receive a storage request event and will save the storage request in
 * {@link StorageRequestStatus#GRANTED GRANTED} status. </li>
 * <li> The {@link FileStorageRequestCheckScheduler} will check if {@link StorageRequestStatus#GRANTED GRANTED} storage requests are to be processed or if
 * the file already exists. If the file exists, the owner of the new request is added to  the file reference,
 * otherwise the request is set to {@link StorageRequestStatus#TO_HANDLE TO_HANDLE} status.
 * This scheduler will also delete requests in {@link StorageRequestStatus#TO_DELETE TO_DELETE} status. </li>
 * <li> The {@link FileStorageRequestDispatchScheduler} will check if {@link StorageRequestStatus#TO_HANDLE TO_HANDLE} storage
 * requests are already being handled. If not, it will create an event to send to file-access to ask for the physical
 * storage of the file. In any case, the request will be set to {@link StorageRequestStatus#HANDLED HANDLED} status.
 * </li>
 * <li> The {@link FileStorageRequestCompleteScheduler} will check if {@link StorageRequestStatus#HANDLED HANDLED} storage
 * requests are completed, meaning that a file reference on the request file already exists. If it exists, the
 * request is set to {@link StorageRequestStatus#TO_DELETE TO_DELETE}. </li>
 * </ul>
 *
 * @author Thibaud Michaudel
 */
@Component
public class FilesStorageRequestEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FilesStorageRequestEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesStorageRequestEventHandler.class);

    private final ISubscriber subscriber;

    private final FileStorageRequestService fileStorageRequestService;

    @Value("${regards.file.catalog.files.storage.request.bulk.size:100}")
    private int bulkSize;

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
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Errors validate(FilesStorageRequestEvent message) {
        return null;
    }

}
