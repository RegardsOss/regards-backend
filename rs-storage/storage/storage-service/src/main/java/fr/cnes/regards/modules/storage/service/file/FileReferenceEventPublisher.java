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
package fr.cnes.regards.modules.storage.service.file;

import com.google.common.collect.Maps;
import fr.cnes.regards.framework.amqp.AbstractPublisher;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.fileaccess.amqp.output.FileAvailableEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.FileReferenceEventType;
import fr.cnes.regards.modules.fileaccess.amqp.output.FileReferenceUpdateEvent;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;

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
        LOGGER.trace("Publishing FileReferenceEvent COPIED. {}", message);
        // Inform all owners of the file that it has been copied in an other storage location.
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(),
                                                   null,
                                                   FileReferenceEventType.COPIED,
                                                   fileRef.getLazzyOwners(),
                                                   message,
                                                   fileRef.getLocation().toDto(),
                                                   fileRef.getMetaInfo().toDto(),
                                                   Sets.newHashSet(groupId)));
    }

    /**
     * Notify listeners for an error processing a {@link FileCopyRequest}.
     * If there is no more {@link FileCopyRequest} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     *
     * @param errorRequest copy request in error
     * @param errorCause   error message
     */
    public void copyError(FileCopyRequest errorRequest, String errorCause) {
        LOGGER.trace("Publishing FileReferenceEvent COPY_ERROR. {}", errorCause);
        publisher.publish(FileReferenceEvent.build(errorRequest.getMetaInfo().getChecksum(),
                                                   null,
                                                   FileReferenceEventType.COPY_ERROR,
                                                   null,
                                                   errorCause,
                                                   new FileLocation(errorRequest.getStorage(),
                                                                    errorRequest.getStorageSubDirectory(),
                                                                    false).toDto(),
                                                   null,
                                                   Sets.newHashSet(errorRequest.getGroupId())));
    }

    /**
     * Notify listeners for a {@link FileReference} deleted successfully for all owners.
     *
     * @param fileRef {@link FileReference} deleted
     * @param message Optional message
     */
    public void deletionSuccess(FileReference fileRef, String message, String groupId) {
        LOGGER.trace("Publishing FileReferenceEvent FULLY_DELETED. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(),
                                                   fileRef.getLocation().getStorage(),
                                                   FileReferenceEventType.FULLY_DELETED,
                                                   null,
                                                   message,
                                                   fileRef.getLocation().toDto(),
                                                   fileRef.getMetaInfo().toDto(),
                                                   Sets.newHashSet(groupId)));
    }

    /**
     * Notify listeners for a {@link FileReference} deletion error.
     *
     * @param fileRef {@link FileReference} not deleted
     * @param message Optional error cause message
     */
    public void deletionError(FileReference fileRef, String message, String groupId) {
        LOGGER.trace("Publishing FileReferenceEvent DELETION_ERROR. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(),
                                                   fileRef.getLocation().getStorage(),
                                                   FileReferenceEventType.DELETION_ERROR,
                                                   null,
                                                   message,
                                                   fileRef.getLocation().toDto(),
                                                   fileRef.getMetaInfo().toDto(),
                                                   Sets.newHashSet(groupId)));
    }

    /**
     * Notify listeners for a {@link FileReference} deleted successfully for one owner.
     *
     * @param fileRef {@link FileReference} deleted for the given owner
     * @param message Optional message
     */
    public void deletionForOwnerSuccess(FileReference fileRef, String owner, String message, String groupId) {
        LOGGER.trace("Publishing FileReferenceEvent DELETED_FOR_OWNER. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(),
                                                   fileRef.getLocation().getStorage(),
                                                   FileReferenceEventType.DELETED_FOR_OWNER,
                                                   Sets.newHashSet(owner),
                                                   message,
                                                   fileRef.getLocation().toDto(),
                                                   fileRef.getMetaInfo().toDto(),
                                                   Sets.newHashSet(groupId)));
    }

    /**
     * Notify listeners for a {@link FileReference} successfully referenced.
     * If there is no more {@link FileStorageRequestAggregation} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     *
     * @param fileRef {@link FileReference} deleted for the given owner
     * @param message Optional message
     */
    public void storeSuccess(FileReference fileRef,
                             String message,
                             Collection<String> groupIds,
                             Collection<String> owners) {
        LOGGER.trace("Publishing FileReferenceEvent STORED. {}", message);
        publisher.publish(FileReferenceEvent.build(fileRef.getMetaInfo().getChecksum(),
                                                   null,
                                                   FileReferenceEventType.STORED,
                                                   owners,
                                                   message,
                                                   fileRef.getLocation().toDto(),
                                                   fileRef.getMetaInfo().toDto(),
                                                   groupIds));
    }

    /**
     * Notify listeners for a {@link FileReference} successfully referenced.
     * If there is no more {@link FileStorageRequestAggregation} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     *
     * @param fileRef {@link FileReference} deleted for the given owner
     * @param message Optional message
     */
    public void storeSuccess(FileReference fileRef, String message, String groupId, Collection<String> owners) {
        storeSuccess(fileRef, message, Sets.newHashSet(groupId), owners);
    }

    /**
     * Notify listeners for an error during a {@link FileReference} referencing.
     * If there is no more {@link FileStorageRequestAggregation} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     *
     * @param checksum of the file in error
     * @param owners   owners of the file in error
     * @param storage  target destination
     * @param message  Optional message
     */
    public void storeError(String checksum,
                           Collection<String> owners,
                           String storage,
                           String message,
                           Collection<String> groupIds) {
        LOGGER.trace("Publishing FileReferenceEvent STORE_ERROR. {}", message);
        publisher.publish(FileReferenceEvent.build(checksum,
                                                   null,
                                                   FileReferenceEventType.STORE_ERROR,
                                                   owners,
                                                   message,
                                                   new FileLocation(storage, null, false).toDto(),
                                                   null,
                                                   groupIds));
    }

    /**
     * Notify listeners for an error during a {@link FileReference} referencing.
     * If there is no more {@link FileStorageRequestAggregation} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     *
     * @param checksum of the file in error
     * @param owners   owners of the file in error
     * @param storage  target destination
     * @param message  Optional message
     */
    public void storeError(String checksum, Collection<String> owners, String storage, String message, String groupId) {
        storeError(checksum, owners, storage, message, Sets.newHashSet(groupId));
    }

    /**
     * Notify listeners for a file available for download.
     * If there is no more {@link FileStorageRequestAggregation} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     */
    public void available(String checksum,
                          String availableStorage,
                          String originStorage,
                          URL url,
                          Collection<String> owners,
                          String message,
                          String groupId,
                          @Nullable OffsetDateTime expirationDate) {
        LOGGER.trace("Publishing FileReferenceEvent AVAILABLE. {}", message);
        publisher.publish(FileReferenceEvent.build(checksum,
                                                   originStorage,
                                                   FileReferenceEventType.AVAILABLE,
                                                   owners,
                                                   message,
                                                   new FileLocation(availableStorage, url.toString(), false).toDto(),
                                                   null,
                                                   Sets.newHashSet(groupId)));
        publisher.broadcast(FileAvailableEvent.EXCHANGE_NAME,
                            Optional.empty(),
                            Optional.of(FileAvailableEvent.ROUTING_KEY_AVAILABILITY_STATUS),
                            Optional.empty(),
                            AbstractPublisher.DEFAULT_PRIORITY,
                            FileAvailableEvent.build(checksum, true, expirationDate),
                            Maps.newHashMap());
    }

    public void updated(String checksum, String storage, FileReference updatedFile) {
        LOGGER.trace("Publishing FileReferenceUpdateEvent for file checksum {} and storage location {}",
                     checksum,
                     storage);
        publisher.publish(new FileReferenceUpdateEvent(checksum, storage, updatedFile.toDto()));
    }

    /**
     * Notify listeners for an restoring a file for download availability.
     * If there is no more {@link FileStorageRequestAggregation} associated to the Business request identifier, so a request notification
     * is sent too.<br/>
     */
    public void notAvailable(String checksum, String originStorage, String message, String groupId) {
        LOGGER.trace("Publishing FileReferenceEvent AVAILABILITY_ERROR. {}", message);
        publisher.publish(FileReferenceEvent.build(checksum,
                                                   originStorage,
                                                   FileReferenceEventType.AVAILABILITY_ERROR,
                                                   null,
                                                   message,
                                                   null,
                                                   null,
                                                   Sets.newHashSet(groupId)));
    }

}
