/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.service.job;

import fr.cnes.regards.modules.storage.client.FileReferenceEventDTO;
import fr.cnes.regards.modules.storage.client.FileReferenceUpdateDTO;
import fr.cnes.regards.modules.storage.client.IStorageFileListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handle storage AMQP message that can be received. Empty methods concerns messages that are of no importance for rs-order
 *
 * @author Sébastien Binda
 */
@Service
public class StorageFileListenerService implements IStorageFileListener, IStorageFileListenerService {

    private final Set<StorageFilesJob> subscribers = ConcurrentHashMap.newKeySet();

    @Override
    public void onFileAvailable(List<FileReferenceEventDTO> available) {
        for (FileReferenceEventDTO event : available) {
            subscribers.forEach(subscriber -> subscriber.handleFileEvent(event.getChecksum(), true));
        }
    }

    @Override
    public void onFileNotAvailable(List<FileReferenceEventDTO> availabilityError) {
        for (FileReferenceEventDTO event : availabilityError) {
            subscribers.forEach(subscriber -> subscriber.handleFileEvent(event.getChecksum(), false));
        }
    }

    @Override
    public void subscribe(StorageFilesJob newSubscriber) {
        subscribers.add(newSubscriber);
    }

    @Override
    public void unsubscribe(StorageFilesJob unscriber) {
        subscribers.remove(unscriber);
    }

    @Override
    public void onFileStored(List<FileReferenceEventDTO> stored) {
        // Do nothing because message is of no importance for rs-order
    }

    @Override
    public void onFileStoreError(List<FileReferenceEventDTO> storedError) {
        // Do nothing because message is of no importance for rs-order
    }

    @Override
    public void onFileDeletedForOwner(String owner, List<FileReferenceEventDTO> deletedForThisOwner) {
        // Do nothing because message is of no importance for rs-order
    }

    @Override
    public void onFileUpdated(List<FileReferenceUpdateDTO> updatedReferences) {
        // Do nothing because message is of no importance for rs-order
    }
}
