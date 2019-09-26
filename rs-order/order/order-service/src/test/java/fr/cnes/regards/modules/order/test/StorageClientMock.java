package fr.cnes.regards.modules.order.test;

import java.time.OffsetDateTime;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.storagelight.client.IStorageClient;
import fr.cnes.regards.modules.storagelight.client.IStorageFileListener;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileCopyRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileStorageRequestDTO;

public class StorageClientMock implements IStorageClient {
	
	@Autowired
	private IStorageFileListener listener;
	
	private boolean isAvailable = true;
	
	public StorageClientMock(IStorageFileListener listener, boolean isAvailable) {
		super();
		this.listener = listener;
		this.isAvailable = isAvailable;
	}

	@Override
	public RequestInfo store(FileStorageRequestDTO file) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RequestInfo store(Collection<FileStorageRequestDTO> files) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void storeRetry(RequestInfo requestInfo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void storeRetry(Collection<String> owners) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void availabilityRetry(RequestInfo requestInfo) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public RequestInfo reference(FileReferenceRequestDTO file) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RequestInfo reference(Collection<FileReferenceRequestDTO> files) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RequestInfo delete(FileDeletionRequestDTO file) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RequestInfo delete(Collection<FileDeletionRequestDTO> files) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RequestInfo copy(FileCopyRequestDTO file) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RequestInfo copy(Collection<FileCopyRequestDTO> files) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RequestInfo makeAvailable(Collection<String> checksums, OffsetDateTime expirationDate) {
		RequestInfo ri = RequestInfo.build();
		for (String c:checksums) {
			if (!isAvailable) {
				listener.onFileNotAvailable(c, Sets.newHashSet(ri), "");
			} else {
				listener.onFileAvailable(c, Sets.newHashSet(ri));
			}
		}
		return ri;
	}

}
