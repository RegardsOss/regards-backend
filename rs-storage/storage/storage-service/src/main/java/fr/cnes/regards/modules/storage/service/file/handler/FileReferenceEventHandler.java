/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.file.handler;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestType;
import fr.cnes.regards.modules.storage.domain.IUpdateFileReferenceOnAvailable;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.service.file.FileReferenceService;
import fr.cnes.regards.modules.storage.service.file.request.FileCopyRequestService;
import fr.cnes.regards.modules.storage.service.file.request.FileStorageRequestService;
import fr.cnes.regards.modules.storage.service.file.request.RequestsGroupService;
import fr.cnes.regards.modules.storage.service.session.SessionNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * This handler is used internally by the storage service to update file requests when a file event is received.
 * <ul>
 * <li> Updates DELAYED storage request after event on file references deletion. </li>
 * <li> Creates storage request after file available event if a copy request is associated </li>
 * <li> Delete cache file after file stored event if a copy request is associated </li>
 * <li> Updates copy request status after availability or stored file events </li>
 * </ul>
 *
 * @author Sébastien Binda
 */
@Component
public class FileReferenceEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<FileReferenceEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceEventHandler.class);

    @Autowired
    private FileStorageRequestService fileStorageRequestService;

    @Autowired
    private FileCopyRequestService fileCopyRequestService;

    @Autowired
    private FileReferenceService fileReferenceService;

    @Autowired
    private RequestsGroupService reqGrpService;

    @Autowired
    private ISubscriber subscriber;

    @Autowired(required = false)
    private Collection<IUpdateFileReferenceOnAvailable> updateActions;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(FileReferenceEvent.class, this);
    }

    @Override
    public Errors validate(FileReferenceEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<FileReferenceEvent> messages) {
        messages.forEach(this::handle);
    }

    public void handle(FileReferenceEvent event) {
        LOGGER.trace("Handling {}", event.toString());
        switch (event.getType()) {
            case AVAILABLE:
                // Check if a copy request is linked to this available file
                handleFileAvailable(event);
                break;
            case AVAILABILITY_ERROR:
                // Check if a copy request is linked to this available file
                handleFileNotAvailable(event);
                break;
            case DELETED_FOR_OWNER:
            case STORED:
                handleFileStored(event);
                break;
            case STORE_ERROR:
                handleStoreError(event);
                break;
            case FULLY_DELETED:
            case DELETION_ERROR:
            default:
                break;
        }
    }

    /**
     * Handle {@link FileReferenceEvent} for successfully stored file
     */
    private void handleFileStored(FileReferenceEvent event) {
        Optional<FileCopyRequest> request = fileCopyRequestService.search(event);
        if (request.isPresent()) {
            Optional<FileReference> oFileRef = fileReferenceService.searchWithOwners(event.getLocation().getStorage(),
                                                                                     event.getMetaInfo().getChecksum());
            if (oFileRef.isPresent()) {
                fileCopyRequestService.handleSuccess(request.get(), oFileRef.get());
                LOGGER.debug("[STORE SUCCESS {}] New stored file is associated to a copy request {}",
                             event.getChecksum(),
                             request.get().getGroupId());
            } else {
                String errorCause = String.format(
                    "Error no file reference found for newly stored file %s at %s storage location",
                    request.get().getStorage(),
                    request.get().getMetaInfo().getChecksum());
                LOGGER.error(errorCause);
                fileCopyRequestService.handleError(request.get(), errorCause);
            }
        }
    }

    /**
     * Handle {@link FileReferenceEvent} for successfully stored file
     */
    private void handleStoreError(FileReferenceEvent event) {
        Optional<FileCopyRequest> request = fileCopyRequestService.search(event);
        if (request.isPresent()) {
            LOGGER.error("[STORE ERROR {}] File is associated to a copy request {}",
                         event.getChecksum(),
                         request.get().getGroupId());
            fileCopyRequestService.handleError(request.get(), event.getMessage());
        }
    }

    /**
     * Handle {@link FileReferenceEvent} for successfully restored file
     */
    private void handleFileAvailable(FileReferenceEvent event) {
        // Execute file reference updates on availability if any defined
        Optional<FileReferenceMetaInfo> fileRefMeta = handleUpdateOnAvailableProcess(event);
        // Then handle copy process if any
        handleCopyProcess(event, fileRefMeta);
    }

    /**
     * Update process after a file is made available.
     *
     * @param event {@link FileReferenceEvent}
     * @return updated {@link FileReferenceMetaInfo}
     */
    private Optional<FileReferenceMetaInfo> handleUpdateOnAvailableProcess(FileReferenceEvent event) {
        Optional<FileReferenceMetaInfo> fileRefMeta = Optional.ofNullable(event.getMetaInfo() != null ?
                                                                              FileReferenceMetaInfo.buildFromDto(event.getMetaInfo()) :
                                                                              null);
        if (updateActions != null) {
            Optional<FileReference> fileReference = fileReferenceService.search(event.getOriginStorage(),
                                                                                event.getChecksum());
            if (fileReference.isPresent()) {
                FileReference fileRef = fileReference.get();
                for (IUpdateFileReferenceOnAvailable action : updateActions) {
                    Optional<FileReference> oUpdated = updateFileReference(fileRef,
                                                                           FileLocation.buildFromDto(event.getLocation()),
                                                                           action);
                    if (oUpdated.isPresent() && (oUpdated.get().getMetaInfo() != null)) {
                        fileRefMeta = Optional.ofNullable(oUpdated.get().getMetaInfo());
                    }
                }
            }
        }
        return fileRefMeta;
    }

    /**
     * Update the given {@link FileReference} with the custom {@link IUpdateFileReferenceOnAvailable} action.
     *
     * @return updated {@link FileReference}
     */
    private Optional<FileReference> updateFileReference(FileReference fileToUpdate,
                                                        FileLocation fileToUpdateLocation,
                                                        IUpdateFileReferenceOnAvailable updateAction) {
        FileReference updatedFile = null;
        // As update processes can change file checksum, save the original checksum retrieved first
        String checksum = fileToUpdate.getMetaInfo().getChecksum();
        String storage = fileToUpdate.getLocation().getStorage();
        try {
            updatedFile = updateAction.update(fileToUpdate, fileToUpdateLocation);
            if (updatedFile != null) {
                // Retrieve fileReference associated to the updated file
                Optional<FileReference> existingOne = fileReferenceService.search(updatedFile.getLocation()
                                                                                             .getStorage(),
                                                                                  updatedFile.getMetaInfo()
                                                                                             .getChecksum());
                // Check that updated fileReference does not match an other existing fileReference
                if (!existingOne.isPresent() || (existingOne.get().getId().equals(fileToUpdate.getId()))) {
                    updatedFile = fileReferenceService.update(checksum, storage, fileToUpdate);
                    LOGGER.debug("File reference {} updated by action {}", checksum, updateAction.getClass().getName());
                } else {
                    updatedFile = null;
                    LOGGER.warn("File reference update {} ignored. Cause, reference already exists for checksum {}.",
                                updateAction.getClass().getName(),
                                checksum);
                }
            }
        } catch (ModuleException e) {
            LOGGER.error("Error updating File Reference after availability for action  {}. Cause : {}",
                         updateAction.getClass().getName(),
                         e.getMessage());
            LOGGER.error(e.getMessage(), e);
        }
        return Optional.ofNullable(updatedFile);
    }

    /**
     * Handle {@link FileReferenceEvent} for file restoration error
     */
    private void handleFileNotAvailable(FileReferenceEvent event) {
        Optional<FileCopyRequest> request = fileCopyRequestService.search(event);
        if (request.isPresent()) {
            LOGGER.error("[AVAILABILITY ERROR {}] File is associated to a copy request {}",
                         event.getChecksum(),
                         request.get().getGroupId());
            fileCopyRequestService.handleError(request.get(), event.getMessage());
        }
    }

    /**
     * Handle copy process after a file available event received
     *
     * @param availableEvent        {@link FileReferenceEvent} Event of file available
     * @param fileAvailableMetaInfo {@link FileReferenceMetaInfo} meta information of file to store
     */
    private void handleCopyProcess(FileReferenceEvent availableEvent,
                                   Optional<FileReferenceMetaInfo> fileAvailableMetaInfo) {
        // Check if a copy request is associated to the available file. If any, create a new storage request for the available file
        // to the copy request destination location.
        Optional<FileCopyRequest> request = fileCopyRequestService.search(availableEvent);
        if (request.isPresent()) {
            FileCopyRequest copyReq = request.get();
            FileReferenceMetaInfo fileMeta = fileAvailableMetaInfo.orElse(copyReq.getMetaInfo());
            LOGGER.trace("[AVAILABILITY SUCCESS {}] Available file is associated to a copy request {}",
                         availableEvent.getChecksum(),
                         request.get().getGroupId());
            String storageGroupId = UUID.randomUUID().toString();
            // Notify storage request and create a new storage request associated to the copy request
            String sessionOwner = copyReq.getSessionOwner();
            String session = copyReq.getSession();
            sessionNotifier.incrementStoreRequests(sessionOwner, session);
            FileStorageRequestAggregation r = fileStorageRequestService.createNewFileStorageRequestFromCopy(copyReq,
                                                                                                            availableEvent.getOriginStorage(),
                                                                                                            fileMeta,
                                                                                                            availableEvent.getLocation()
                                                                                                                          .getUrl(),
                                                                                                            storageGroupId,
                                                                                                            sessionOwner,
                                                                                                            session);

            copyReq.setFileStorageGroupId(storageGroupId);
            fileCopyRequestService.update(copyReq);
            LOGGER.trace("[COPY REQUEST {}] Storage request is created for successfully restored file",
                         copyReq.getMetaInfo().getChecksum(),
                         copyReq.getGroupId());

            reqGrpService.granted(storageGroupId,
                                  FileRequestType.STORAGE,
                                  1,
                                  true,
                                  fileStorageRequestService.getRequestExpirationDate());
        }
    }

}
