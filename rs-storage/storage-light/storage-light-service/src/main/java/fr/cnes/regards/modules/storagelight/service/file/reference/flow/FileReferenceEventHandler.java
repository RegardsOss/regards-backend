/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.service.file.reference.flow;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileCopyRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileDeletionRequestService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileReferenceService;
import fr.cnes.regards.modules.storagelight.service.file.reference.FileStorageRequestService;

/**
 * This handler is used internally by the storage service to update file requests when a file event is received.
 * <ul>
 * <li> Updates DELAYED storage request after event on file references deletion. </li>
 * <li> Creates storage request after file available event if a copy request is associated </li>
 * <li> Delete cache file after file stored event if a copy request is associated </li>
 * <li> Updates copy request status after availability or stored file events </li>
 * </ul>
 * @author Sébastien Binda
 */
@Component
public class FileReferenceEventHandler
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<FileReferenceEvent> {

    @Autowired
    private FileStorageRequestService fileReferenceRequestService;

    @Autowired
    private FileStorageRequestService fileStorageRequestService;

    @Autowired
    private FileDeletionRequestService fileDeletionRequestService;

    @Autowired
    private FileCopyRequestService fileCopyRequestService;

    @Autowired
    private FileReferenceService fileReferenceService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FileReferenceEvent.class, this);
    }

    @Override
    public void handle(TenantWrapper<FileReferenceEvent> wrapper) {
        String tenant = wrapper.getTenant();
        LOGGER.info("Handling {}", wrapper.getContent().toString());
        runtimeTenantResolver.forceTenant(tenant);
        try {
            switch (wrapper.getContent().getType()) {
                case FULLY_DELETED:
                case DELETION_ERROR:
                    // When a file reference deletion is over, schedule the delayed reference requests if any
                    // Indeed, when a file reference deletion process is running, every file reference request are delayed until
                    // the deletion process is over.
                    scheduleDelayedFileRefRequests(wrapper.getContent().getChecksum(),
                                                   wrapper.getContent().getLocation().getStorage());
                    break;
                case AVAILABLE:
                    // Check if a copy request is linked to this available file
                    handleFileAvailable(wrapper.getContent());
                    break;
                case AVAILABILITY_ERROR:
                    // Check if a copy request is linked to this available file
                    handleFileNotAvailable(wrapper.getContent());
                    break;
                case DELETED_FOR_OWNER:
                case STORED:
                    handleFileStored(wrapper.getContent());
                    break;
                case STORE_ERROR:
                    handleStoreError(wrapper.getContent());
                default:
                    break;
            }
        } finally {
            runtimeTenantResolver.clearTenant();
        }

    }

    /**
     * After a deletion success, we can schedule the file reference request delayed. Those request was waiting for deletion ends.
     */
    private void scheduleDelayedFileRefRequests(String fileRefChecksum, String fileRefStorage) {
        Optional<FileStorageRequest> oRequest = fileReferenceRequestService.search(fileRefStorage, fileRefChecksum);
        if (oRequest.isPresent() && (oRequest.get().getStatus() == FileRequestStatus.DELAYED)) {
            // As a storage is scheduled, we can delete the deletion request
            Optional<FileReference> oFileRef = fileReferenceService.search(fileRefStorage, fileRefChecksum);
            if (oFileRef.isPresent()) {
                Optional<FileDeletionRequest> oDeletionRequest = fileDeletionRequestService.search(oFileRef.get());
                if (oDeletionRequest.isPresent()) {
                    fileDeletionRequestService.delete(oDeletionRequest.get());
                }
            }
            FileStorageRequest request = oRequest.get();
            request.setStatus(FileRequestStatus.TODO);
            fileReferenceRequestService.update(request);
        }
    }

    private void handleFileStored(FileReferenceEvent event) {
        Optional<FileCopyRequest> request = fileCopyRequestService.search(event);
        if (request.isPresent()) {
            Optional<FileReference> oFileRef = fileReferenceService.search(request.get().getStorage(),
                                                                           request.get().getMetaInfo().getChecksum());
            if (oFileRef.isPresent()) {
                fileCopyRequestService.handleSuccess(request.get(), oFileRef.get());
                LOGGER.info("[COPY REQUEST] New stored file {} is associated to copy request {}", event.getChecksum(),
                            request.get().getGroupId());
            } else {
                String errorCause = String
                        .format("Error no file reference found for newly stored file %s at %s storage location",
                                request.get().getStorage(), request.get().getMetaInfo().getChecksum());
                LOGGER.error(errorCause);
                fileCopyRequestService.handleError(request.get(), errorCause);
            }
        }
    }

    private void handleStoreError(FileReferenceEvent event) {
        Optional<FileCopyRequest> request = fileCopyRequestService.search(event);
        if (request.isPresent()) {
            fileCopyRequestService.handleError(request.get(), event.getMessage());
        }
    }

    private void handleFileAvailable(FileReferenceEvent event) {
        Optional<FileCopyRequest> request = fileCopyRequestService.search(event);
        if (request.isPresent()) {
            createNewStorageRequest(request.get(), event);
            LOGGER.info("[COPY REQUEST] Available file {} is associated to copy request {}", event.getChecksum(),
                        request.get().getGroupId());
        }
    }

    private void handleFileNotAvailable(FileReferenceEvent event) {
        Optional<FileCopyRequest> request = fileCopyRequestService.search(event);
        if (request.isPresent()) {
            FileCopyRequest copyRequest = request.get();
            copyRequest.setStatus(FileRequestStatus.ERROR);
            copyRequest.setErrorCause(event.getMessage());
            fileCopyRequestService.update(copyRequest);
            LOGGER.info("[COPY REQUEST] Not Available file {} is associated to copy request {}", event.getChecksum(),
                        request.get().getGroupId());
        }
    }

    private void createNewStorageRequest(FileCopyRequest copyRequest, FileReferenceEvent fileAvailableEvent) {
        String storageGroupId = UUID.randomUUID().toString();
        try {
            Optional<FileStorageRequest> request = fileStorageRequestService
                    .create(fileAvailableEvent.getOwners(), copyRequest.getMetaInfo(),
                            new URL(fileAvailableEvent.getLocation().getUrl()), copyRequest.getStorage(),
                            Optional.ofNullable(copyRequest.getStorageSubDirectory()), storageGroupId);
            if (request.isPresent()) {
                copyRequest.setFileStorageGroupId(storageGroupId);
                fileCopyRequestService.update(copyRequest);
            }
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage(), e);
            copyRequest.setStatus(FileRequestStatus.ERROR);
            copyRequest.setErrorCause(String.format("Restored file is not available at url {}. Cause  : {}",
                                                    fileAvailableEvent.getLocation().getUrl(), e.getMessage()));
            fileCopyRequestService.update(copyRequest);
        }
    }

}
