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
import fr.cnes.regards.modules.storagelight.dao.IFileStorageRequestRepository;
import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventType;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestEvent.ErrorFile;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestEventState;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;

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

    /**
     * Notify listeners for a {@link FileReference} deleted successfully for all owners.
     * @param fileRef {@link FileReference} deleted
     * @param message Optional message
     */
    public void deletionSuccess(FileReference fileRef, String message, String requestId) {
        LOGGER.debug("Publish FileReferenceEvent Deleted. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(),
                                                   FileReferenceEventType.FULLY_DELETED, null, message,
                                                   fileRef.getLocation(), Sets.newHashSet(requestId)));
    }

    /**
     * Notify listeners for a {@link FileReference} deletion error.
     * @param fileRef {@link FileReference} not deleted
     * @param message Optional error cause message
     */
    public void deletionError(FileReference fileRef, String message, String requestId) {
        LOGGER.debug("Publish FileReferenceEvent Delete error. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(),
                                                   FileReferenceEventType.DELETION_ERROR, null, message,
                                                   fileRef.getLocation(), Sets.newHashSet(requestId)));
    }

    /**
     * Notify listeners for a {@link FileReference} deleted successfully for one owner.
     * @param fileRef {@link FileReference} deleted for the given owner
     * @param owner
     * @param message Optional message
     */
    public void deletionForOwnerSuccess(FileReference fileRef, String owner, String message, String requestId) {
        LOGGER.debug("Publish FileReferenceEvent Deleted for owner. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(),
                                                   FileReferenceEventType.DELETED_FOR_OWNER, Sets.newHashSet(owner),
                                                   message, fileRef.getLocation(), Sets.newHashSet(requestId)));
    }

    /**
     * Notify listeners for a {@link FileReference} successfully referenced.
     * @param fileRef {@link FileReference} deleted for the given owner
     * @param message Optional message
     */
    public void storeSuccess(FileReference fileRef, String message, Collection<String> requestIds) {
        LOGGER.debug("Publish FileReferenceEvent stored. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(), FileReferenceEventType.STORED,
                                                   fileRef.getOwners(), message, fileRef.getLocation(), requestIds));
        for (String requestId : requestIds) {
            if (!storageReqRepo.existsByRequestIds(requestId)) {
                requestDone(requestId, FileRequestType.STORAGE);
            } else {
                checkForStorageRequestError(requestId);
            }
        }
    }

    /**
     * Notify listeners for a {@link FileReference} successfully referenced.
     * @param fileRef {@link FileReference} deleted for the given owner
     * @param message Optional message
     */
    public void storeSuccess(FileReference fileRef, String message, String requestId) {
        storeSuccess(fileRef, message, Sets.newHashSet(requestId));
    }

    /**
     * Notify listeners for an error during a {@link FileReference} referencing.
     * @param checksum of the file in error
     * @param owners owners of the file in error
     * @param destinationLocation
     * @param message Optional message
     */
    public void storeError(String checksum, Collection<String> owners, String storage, String message,
            Collection<String> requestIds) {
        LOGGER.debug("Publish FileReferenceEvent store error. {}", message);
        publisher.publish(FileReferenceEvent.build(checksum, FileReferenceEventType.STORE_ERROR, owners, message,
                                                   new FileLocation(storage, null), requestIds));
        requestIds.forEach(r -> checkForStorageRequestError(r));
    }

    public void storeError(String checksum, Collection<String> owners, String storage, String message,
            String requestId) {
        storeError(checksum, owners, storage, message, Sets.newHashSet(requestId));
    }

    private void checkForStorageRequestError(String requestId) {
        Set<FileStorageRequest> requests = storageReqRepo.findByRequestIds(requestId);
        // If all remaining requests are in error state, publish request in error
        if (!requests.stream().anyMatch(req -> !(req.getStatus() == FileRequestStatus.ERROR))) {
            Set<ErrorFile> errors = requests.stream()
                    .map(req -> ErrorFile.build(req.getMetaInfo().getChecksum(), req.getStorage(), req.getErrorCause()))
                    .collect(Collectors.toSet());
            requestError(requestId, FileRequestType.STORAGE, errors);
        }
    }

    public void available(String checksum, String message, String requestId, boolean notifyRequest) {
        LOGGER.debug("Publish FileReferenceEvent available for download. {}", message);
        publisher.publish(FileReferenceEvent.build(checksum, FileReferenceEventType.AVAILABLE, null, message, null,
                                                   Sets.newHashSet(requestId)));

        // Check if cache request exists for the same requestId
        if (notifyRequest) {
            if (!cacheReqRepo.existsByRequestId(requestId)) {
                requestDone(requestId, FileRequestType.AVAILABILITY);
            } else {
                checkForAvailabilityRequestError(requestId);
            }
        }
    }

    public void notAvailable(String checksum, String message, String requestId, boolean notifyRequest) {
        LOGGER.debug("Publish FileReferenceEvent not available for download. {}", message);
        publisher.publish(FileReferenceEvent.build(checksum, FileReferenceEventType.AVAILABILITY_ERROR, null, message,
                                                   null, Sets.newHashSet(requestId)));
        if (notifyRequest) {
            checkForAvailabilityRequestError(requestId);
        }
    }

    private void checkForAvailabilityRequestError(String requestId) {
        Set<FileCacheRequest> requests = cacheReqRepo.findByRequestId(requestId);
        // If all remaining requests are in error state, publish request in error
        if (!requests.stream().anyMatch(req -> !(req.getStatus() == FileRequestStatus.ERROR))) {
            Set<ErrorFile> errors = requests.stream()
                    .map(req -> ErrorFile.build(req.getChecksum(), req.getStorage(), req.getErrorCause()))
                    .collect(Collectors.toSet());
            requestError(requestId, FileRequestType.AVAILABILITY, errors);
        }
    }

    public void requestDenied(String requestId, FileRequestType type) {
        publisher.publish(FileRequestEvent.build(requestId, type, FileRequestEventState.DENIED));
    }

    public void requestGranted(String requestId, FileRequestType type) {
        publisher.publish(FileRequestEvent.build(requestId, type, FileRequestEventState.GRANTED));
    }

    public void requestDone(String requestId, FileRequestType type) {
        publisher.publish(FileRequestEvent.build(requestId, type, FileRequestEventState.DONE));
    }

    public void requestError(String requestId, FileRequestType type, Collection<ErrorFile> errors) {
        publisher.publish(FileRequestEvent.buildError(requestId, type, errors));
    }

}
