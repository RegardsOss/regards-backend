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

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.storagelight.dao.IFileCacheRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileCopyRequestRepository;
import fr.cnes.regards.modules.storagelight.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventType;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceUpdateEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestEvent.ErrorFile;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import fr.cnes.regards.modules.storagelight.domain.flow.FlowItemStatus;

/**
 * Publisher to send AMQP message notification when there is any change on a File Reference.
 *
 * @author Sébastien Binda
 */
@Component
public class FileReferenceEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReferenceEventPublisher.class);

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IFileStorageRequestRepository storageReqRepo;

    @Autowired
    private IFileCacheRequestRepository cacheReqRepo;

    @Autowired
    private IFileCopyRequestRepository copyRepo;

    /**
     * Notify listeners for a {@link FileReference} copied to a new storage location.
     * If there is no more {@link FileCopyRequest} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     *
     * @param fileRef newly stored file
     * @param message success message
     * @param groupId Business request identifier
     */
    public void copySuccess(FileReference fileRef, String message, String groupId) {
        LOGGER.debug("Publishing FileReferenceEvent COPIED. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(), FileReferenceEventType.COPIED,
                                                   fileRef.getOwners(), message, fileRef.getLocation(),
                                                   Sets.newHashSet(groupId)));
        if (!copyRepo.existsByGroupId(groupId)) {
            // No copy request left, request is successfully done
            LOGGER.info("[COPY REQUEST] No copy request left, request is successfully done for request id {}", groupId);
            requestDone(groupId, FileRequestType.COPY);
        } else {
            checkForCopyRequestError(groupId);
        }
    }

    /**
     * Notify listeners for an error processing a {@link FileCopyRequest}.
     * If there is no more {@link FileCopyRequest} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     *
     * @param errorRequest copy request in error
     * @param errorCause error message
     */
    public void copyError(FileCopyRequest errorRequest, String errorCause) {
        LOGGER.debug("Publishing FileReferenceEvent COPY_ERROR. {}", errorCause);
        publisher.publish(FileReferenceEvent
                .build(errorRequest.getMetaInfo().getChecksum(), FileReferenceEventType.COPY_ERROR, null, errorCause,
                       new FileLocation(errorRequest.getStorage(), errorRequest.getStorageSubDirectory()),
                       Sets.newHashSet(errorRequest.getGroupId())));
        checkForCopyRequestError(errorRequest.getGroupId());
    }

    /**
     * Notify listeners for a {@link FileReference} deleted successfully for all owners.
     *
     * @param fileRef {@link FileReference} deleted
     * @param message Optional message
     * @param groupId
     */
    public void deletionSuccess(FileReference fileRef, String message, String groupId) {
        LOGGER.debug("Publishing FileReferenceEvent FULLY_DELETED. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(),
                                                   FileReferenceEventType.FULLY_DELETED, null, message,
                                                   fileRef.getLocation(), Sets.newHashSet(groupId)));
    }

    /**
     * Notify listeners for a {@link FileReference} deletion error.
     *
     * @param fileRef {@link FileReference} not deleted
     * @param message Optional error cause message
     * @param groupId
     */
    public void deletionError(FileReference fileRef, String message, String groupId) {
        LOGGER.debug("Publishing FileReferenceEvent DELETION_ERROR. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(),
                                                   FileReferenceEventType.DELETION_ERROR, null, message,
                                                   fileRef.getLocation(), Sets.newHashSet(groupId)));
    }

    /**
     * Notify listeners for a {@link FileReference} deleted successfully for one owner.
     * @param fileRef {@link FileReference} deleted for the given owner
     * @param owner
     * @param message Optional message
     * @param groupId
     */
    public void deletionForOwnerSuccess(FileReference fileRef, String owner, String message, String groupId) {
        LOGGER.debug("Publishing FileReferenceEvent DELETED_FOR_OWNER. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(),
                                                   FileReferenceEventType.DELETED_FOR_OWNER, Sets.newHashSet(owner),
                                                   message, fileRef.getLocation(), Sets.newHashSet(groupId)));
    }

    /**
     * Notify listeners for a {@link FileReference} successfully referenced.
     * If there is no more {@link FileStorageRequest} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     *
     * @param fileRef {@link FileReference} deleted for the given owner
     * @param message Optional message
     * @param groupIds
     */
    public void storeSuccess(FileReference fileRef, String message, Collection<String> groupIds) {
        LOGGER.debug("Publishing FileReferenceEvent STORED. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(), FileReferenceEventType.STORED,
                                                   fileRef.getOwners(), message, fileRef.getLocation(), groupIds));
        for (String groupId : groupIds) {
            if (!storageReqRepo.existsByGroupIds(groupId)) {
                requestDone(groupId, FileRequestType.STORAGE);
            } else {
                checkForStorageRequestError(groupId);
            }
        }
    }

    /**
     * Notify listeners for a {@link FileReference} successfully referenced.
     * If there is no more {@link FileStorageRequest} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     *
     * @param fileRef {@link FileReference} deleted for the given owner
     * @param message Optional message
     * @param groupId
     */
    public void storeSuccess(FileReference fileRef, String message, String groupId) {
        storeSuccess(fileRef, message, Sets.newHashSet(groupId));
    }

    /**
     * Notify listeners for an error during a {@link FileReference} referencing.
     * If there is no more {@link FileStorageRequest} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     *
     * @param checksum of the file in error
     * @param owners owners of the file in error
     * @param storage target destination
     * @param message Optional message
     * @param groupIds
     */
    public void storeError(String checksum, Collection<String> owners, String storage, String message,
            Collection<String> groupIds) {
        LOGGER.debug("Publishing FileReferenceEvent STORE_ERROR. {}", message);
        publisher.publish(FileReferenceEvent.build(checksum, FileReferenceEventType.STORE_ERROR, owners, message,
                                                   new FileLocation(storage, null), groupIds));
        groupIds.forEach(r -> checkForStorageRequestError(r));
    }

    /**
     * Notify listeners for an error during a {@link FileReference} referencing.
     * If there is no more {@link FileStorageRequest} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     *
     * @param checksum of the file in error
     * @param owners owners of the file in error
     * @param storage target destination
     * @param message Optional message
     * @param groupId
     */
    public void storeError(String checksum, Collection<String> owners, String storage, String message, String groupId) {
        storeError(checksum, owners, storage, message, Sets.newHashSet(groupId));
    }

    private void checkForStorageRequestError(String groupId) {
        Set<FileStorageRequest> requests = storageReqRepo.findByGroupIds(groupId);
        // If all remaining requests are in error state, publish request in error
        if (!requests.stream().anyMatch(req -> !(req.getStatus() == FileRequestStatus.ERROR))) {
            Set<ErrorFile> errors = requests.stream()
                    .map(req -> ErrorFile.build(req.getMetaInfo().getChecksum(), req.getStorage(), req.getErrorCause()))
                    .collect(Collectors.toSet());
            requestError(groupId, FileRequestType.STORAGE, errors);
        }
    }

    /**
     * Notify listeners for a file available for download.
     * If there is no more {@link FileStorageRequest} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     * @param checksum
     * @param storage
     * @param url
     * @param owners
     * @param message
     * @param groupId
     * @param notifyRequest
     */
    public void available(String checksum, String storage, String url, Collection<String> owners, String message,
            String groupId, Boolean notifyRequest) {
        LOGGER.debug("Publishing FileReferenceEvent AVAILABLE. {}", message);
        publisher.publish(FileReferenceEvent.build(checksum, FileReferenceEventType.AVAILABLE, owners, message,
                                                   new FileLocation(storage, url), Sets.newHashSet(groupId)));

        // Check if cache request exists for the same groupId
        if (notifyRequest) {
            if (!cacheReqRepo.existsByGroupId(groupId)) {
                requestDone(groupId, FileRequestType.AVAILABILITY);
            } else {
                checkForAvailabilityRequestError(groupId);
            }
        }
    }

    public void updated(String checksum, String storage, FileReference updatedFile) {
        LOGGER.debug("Publishing FileReferenceUpdateEvent for file checksum {} and storage location {}", checksum,
                     storage);
        publisher.publish(FileReferenceUpdateEvent.build(checksum, storage, updatedFile));
    }

    /**
     * Notify listeners for an restoring a file for download availability.
     * If there is no more {@link FileStorageRequest} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     *
     * @param checksum
     * @param message
     * @param groupId
     * @param notifyRequest
     */
    public void notAvailable(String checksum, String message, String groupId, Boolean notifyRequest) {
        LOGGER.debug("Publishing FileReferenceEvent AVAILABILITY_ERROR. {}", message);
        publisher.publish(FileReferenceEvent.build(checksum, FileReferenceEventType.AVAILABILITY_ERROR, null, message,
                                                   null, Sets.newHashSet(groupId)));
        if (notifyRequest) {
            checkForAvailabilityRequestError(groupId);
        }
    }

    private void checkForAvailabilityRequestError(String groupId) {
        Set<FileCacheRequest> requests = cacheReqRepo.findByGroupId(groupId);
        // If all remaining requests are in error state, publish request in error
        if (!requests.stream().anyMatch(req -> !(req.getStatus() == FileRequestStatus.ERROR))) {
            Set<ErrorFile> errors = requests.stream()
                    .map(req -> ErrorFile.build(req.getChecksum(), req.getStorage(), req.getErrorCause()))
                    .collect(Collectors.toSet());
            requestError(groupId, FileRequestType.AVAILABILITY, errors);
        }
    }

    private void checkForCopyRequestError(String groupId) {
        Set<FileCopyRequest> requests = copyRepo.findByGroupId(groupId);
        // If all remaining requests are in error state, publish request in error
        if (!requests.stream().anyMatch(req -> !(req.getStatus() == FileRequestStatus.ERROR))) {
            Set<ErrorFile> errors = requests.stream()
                    .map(req -> ErrorFile.build(req.getMetaInfo().getChecksum(), req.getStorage(), req.getErrorCause()))
                    .collect(Collectors.toSet());
            requestError(groupId, FileRequestType.COPY, errors);
        }
    }

    public void requestDenied(String groupId, FileRequestType type) {
        publisher.publish(FileRequestEvent.build(groupId, type, FlowItemStatus.DENIED));
    }

    public void requestGranted(String groupId, FileRequestType type) {
        publisher.publish(FileRequestEvent.build(groupId, type, FlowItemStatus.GRANTED));
    }

    public void requestDone(String groupId, FileRequestType type) {
        publisher.publish(FileRequestEvent.build(groupId, type, FlowItemStatus.DONE));
    }

    public void requestError(String groupId, FileRequestType type, Collection<ErrorFile> errors) {
        publisher.publish(FileRequestEvent.buildError(groupId, type, errors));
    }

}
