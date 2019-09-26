package fr.cnes.regards.modules.order.service.job;

/**
 * Service for scrubscribe to the StorageFileLitener 
 * @author Kevin Marchois
 *
 */
public interface IStorageFileListenerService {

	void subscribe(StorageFilesJob newSubscriber);

	void unsubscribe(StorageFilesJob unscriber);

}