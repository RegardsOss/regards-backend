package fr.cnes.regards.modules.order.service.job;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.storage.client.FileReferenceEventDTO;
import fr.cnes.regards.modules.storage.client.FileReferenceUpdateDTO;
import fr.cnes.regards.modules.storage.client.IStorageFileListener;

/**
 * Handle storage AMQP message that can be received. Empty methods concerns messages that are of no importance for rs-order
 */
@Service
public class StorageFileListenerService implements IStorageFileListener, IStorageFileListenerService {

    private final Set<StorageFilesJob> subscribers = new HashSet<>();

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
    }

    @Override
    public void onFileStoreError(List<FileReferenceEventDTO> storedError) {
    }

    @Override
    public void onFileDeletedForOwner(String owner, List<FileReferenceEventDTO> deletedForThisOwner) {
    }

    @Override
    public void onFileUpdated(List<FileReferenceUpdateDTO> updatedReferences) {
    }
}
