package fr.cnes.regards.modules.order.service.job;

/**
 * Service for subscribe to the StorageFileListener
 * @author Kevin Marchois
 *
 */
public interface IStorageFileListenerService {

	void subscribe(StorageFilesJob newSubscriber);

	void unsubscribe(StorageFilesJob unscriber);

}