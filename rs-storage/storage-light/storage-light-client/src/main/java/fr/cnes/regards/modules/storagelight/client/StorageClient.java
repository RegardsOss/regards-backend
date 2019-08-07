/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.client;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.storagelight.domain.flow.AddFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.AvailabilityFileRefFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.DeleteFileRefFlowItem;

/**
 * Asynchronous client implementation based on the message broker for requesting the file storage service
 *
 * @author Marc SORDI
 *
 */
@Component
public class StorageClient implements IStorageClient {

    @Autowired
    private IPublisher publisher;

    @Override
    public void copy(String fileName, String checksum, String owner, String storage, Optional<String> subDirectory) {
        // TODO
    }

    @Override
    public void delete(String checksum, String storage, String owner) {
        publisher.publish(new DeleteFileRefFlowItem(checksum, storage, owner));
    }

    @Override
    public void makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate) {
        publisher.publish(new AvailabilityFileRefFlowItem(checksums, expirationDate));
    }

    @Override
    public void reference(String fileName, String checksum, String algorithm, String mimeType, Long fileSize,
            String owner, String storage, String url) {
        publisher.publish(AddFileRefFlowItem.build(fileName, checksum, algorithm, mimeType, fileSize, owner, storage,
                                                   url));
    }

    @Override
    public void store(String fileName, String checksum, String algorithm, String mimeType, String owner, URL originUrl,
            String storage, Optional<String> subDirectory) {
        AddFileRefFlowItem item = AddFileRefFlowItem.build(fileName, checksum, algorithm, mimeType, owner, storage,
                                                           originUrl);
        if (subDirectory.isPresent()) {
            item.storeIn(subDirectory.get());
        }
        publisher.publish(item);
    }

}
