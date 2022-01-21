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
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.storage.client.FileReferenceEventDTO;
import fr.cnes.regards.modules.storage.client.FileReferenceUpdateDTO;
import fr.cnes.regards.modules.storage.client.IStorageFileListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
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
public class StorageFileListenerService implements IStorageFileListener, IStorageFileListenerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageFileListenerService.class);

    private final Set<StorageFilesJob> subscribers = ConcurrentHashMap.newKeySet();

    private StorageFileListenerService self;

    private final ApplicationContext applicationContext;

    public StorageFileListenerService(ApplicationContext applicationContext){
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void post() {
        self = applicationContext.getBean(this.getClass());
    }

    @Override
    public void onFileAvailable(List<FileReferenceEventDTO> available) {
        Set<String> availableChecksums = available.stream().map(FileReferenceEventDTO::getChecksum).collect(Collectors.toSet());
        subscribers.forEach(subscriber -> this.changeFilesStateWithRetry(availableChecksums, FileState.AVAILABLE, subscriber, 5));
    }

    @Override
    public void onFileNotAvailable(List<FileReferenceEventDTO> availabilityError) {
        Set<String> inErrorChecksum = availabilityError.stream().map(FileReferenceEventDTO::getChecksum).collect(Collectors.toSet());
        subscribers.forEach(subscriber -> this.changeFilesStateWithRetry(inErrorChecksum, FileState.ERROR, subscriber, 5));
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

    private void changeFilesStateWithRetry(Set<String> checksums, FileState state, StorageFilesJob jobSubscriber, int nbRetry) {
        try {
            self.changeFilesState(checksums, state, jobSubscriber);
        } catch (ObjectOptimisticLockingFailureException e) {
            if (nbRetry > 0) {
                LOGGER.trace("Another schedule has updated some of the order files handled by this method while it was running.", e);
                // we retry until it succeed because if it does not succeed on first time it is most likely because of
                // another scheduled method that would then most likely happen at next invocation because execution delays are fixed
                // Moreover, we cannot retry on the same content as it has to be reloaded from DB
                this.changeFilesStateWithRetry(checksums, state, jobSubscriber, nbRetry-1);
            } else {
                throw e;
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void changeFilesState(Set<String> checksums, FileState state, StorageFilesJob jobSubscriber) {
        jobSubscriber.changeFilesState(checksums, state);
    }
}
