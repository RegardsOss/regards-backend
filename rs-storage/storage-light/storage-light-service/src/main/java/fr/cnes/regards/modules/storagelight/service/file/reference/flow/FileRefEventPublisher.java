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

import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEventState;

/**
 * @author sbinda
 *
 */
@Component
public class FileRefEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRefEventPublisher.class);

    @Autowired
    private IPublisher publisher;

    public void publishFileRefDeleted(FileReference fileRef, String message) {
        LOGGER.debug("Publish FileReferenceEvent Deleted. {}", message);
        publisher.publish(new FileReferenceEvent(fileRef.getMetaInfo().getChecksum(),
                FileReferenceEventState.FULLY_DELETED, null, message, fileRef.getLocation()));
    }

    public void publishFileRefDeletionError(FileReference fileRef, String message) {
        LOGGER.debug("Publish FileReferenceEvent Delete error. {}", message);
        publisher.publish(new FileReferenceEvent(fileRef.getMetaInfo().getChecksum(),
                FileReferenceEventState.DELETION_ERROR, null, message, fileRef.getLocation()));
    }

    public void publishFileRefDeletedForOwner(FileReference fileRef, String owner, String message) {
        LOGGER.debug("Publish FileReferenceEvent Deleted for owner. {}", message);
        publisher.publish(new FileReferenceEvent(fileRef.getMetaInfo().getChecksum(),
                FileReferenceEventState.DELETED_FOR_OWNER, Sets.newHashSet(owner), message, fileRef.getLocation()));
    }

    public void publishFileRefStored(FileReference fileRef, String message) {
        LOGGER.debug("Publish FileReferenceEvent stored. {}", message);
        publisher.publish(new FileReferenceEvent(fileRef.getMetaInfo().getChecksum(), FileReferenceEventState.STORED,
                fileRef.getOwners(), message, fileRef.getLocation()));
    }

    public void publishFileRefStoreError(String checksum, Collection<String> owners, FileLocation destinationLocation,
            String message) {
        LOGGER.debug("Publish FileReferenceEvent store error. {}", message);
        publisher.publish(new FileReferenceEvent(checksum, FileReferenceEventState.STORE_ERROR, owners, message,
                destinationLocation));
    }

}
