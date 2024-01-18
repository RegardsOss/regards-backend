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
package fr.cnes.regards.modules.order.service.job;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.fileaccess.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceUpdateDto;
import fr.cnes.regards.modules.filecatalog.client.listener.IStorageFileListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Handle storage AMQP message that can be received. Empty methods concerns messages that are of no importance for rs-order
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class StorageFileListenerService implements IStorageFileListener, IStorageFileListenerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageFileListenerService.class);

    private final Set<StorageFilesJob> subscribers = ConcurrentHashMap.newKeySet();

    private final StorageFileListenerService self;

    private final ApplicationContext applicationContext;

    public StorageFileListenerService(ApplicationContext applicationContext,
                                      StorageFileListenerService storageFileListenerService) {
        this.applicationContext = applicationContext;
        this.self = storageFileListenerService;
    }

    @Override
    public void onFileAvailable(List<FileReferenceEvent> available) {
        Set<String> availableChecksums = available.stream()
                                                  .map(FileReferenceEvent::getChecksum)
                                                  .collect(Collectors.toSet());
        subscribers.forEach(subscriber -> subscriber.notifyFilesAvailable(availableChecksums));
    }

    @Override
    public void onFileNotAvailable(List<FileReferenceEvent> availabilityError) {
        Set<String> inErrorChecksum = availabilityError.stream()
                                                       .map(FileReferenceEvent::getChecksum)
                                                       .collect(Collectors.toSet());
        subscribers.forEach(subscriber -> subscriber.notifyFilesUnavailable(inErrorChecksum));
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
    public void onFileStored(List<FileReferenceEvent> stored) {
        // Do nothing because message is of no importance for rs-order
    }

    @Override
    public void onFileStoreError(List<FileReferenceEvent> storedError) {
        // Do nothing because message is of no importance for rs-order
    }

    @Override
    public void onFileDeletedForOwner(String owner, List<FileReferenceEvent> deletedForThisOwner) {
        // Do nothing because message is of no importance for rs-order
    }

    @Override
    public void onFileUpdated(List<FileReferenceUpdateDto> updatedReferences) {
        // Do nothing because message is of no importance for rs-order
    }
}
