package fr.cnes.regards.modules.order.service.job;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.storagelight.client.IStorageFileListener;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;

@Service
public class StorageFileListenerService implements IStorageFileListener, IStorageFileListenerService {

	private final Set<StorageFilesJob> subscribers = new HashSet<> ();
	
	@Override
	public void onFileStored(String checksum, String storage, Collection<String> owners,
			Collection<RequestInfo> requestInfos) {
	}

	@Override
	public void onFileStoreError(String checksum, String storage, Collection<String> owners,
			Collection<RequestInfo> requestInfos, String errorCause) {
	}

	@Override
	public void onFileAvailable(String checksum, Collection<RequestInfo> requestInfos) {
		subscribers.stream().forEach(subscriber -> subscriber.handle(checksum, true));
	}

	@Override
	public void onFileNotAvailable(String checksum, Collection<RequestInfo> requestInfos, String errorCause) {
		subscribers.stream().forEach(subscriber -> subscriber.handle(checksum, false));
	}

	@Override
	public void onFileDeleted(String checksum, String storage, String owner, Collection<RequestInfo> requestInfos) {
	}

	@Override
	public void onFileUpdated(String checksum, String storage, FileReference updateFile) {

	}

	@Override
	public void subscribe(StorageFilesJob newSubscriber) {
		subscribers.add(newSubscriber);
	}

	@Override
	public void unsubscribe(StorageFilesJob unscriber) {
		subscribers.remove(unscriber);
	}

}
