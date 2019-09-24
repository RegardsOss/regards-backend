package fr.cnes.regards.modules.search.rest.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.springframework.context.annotation.Primary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.storagelight.client.IStorageFileListener;
import fr.cnes.regards.modules.storagelight.client.IStorageRestClient;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;

@Primary
@Service
public class IStorageRestClientMock implements IStorageRestClient, IStorageFileListener {

	@Override
	public void onFileStored(String checksum, String storage, Collection<String> owners,
			Collection<RequestInfo> requestInfos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFileStoreError(String checksum, String storage, Collection<String> owners,
			Collection<RequestInfo> requestInfos, String errorCause) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFileAvailable(String checksum, Collection<RequestInfo> requestInfos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFileNotAvailable(String checksum, Collection<RequestInfo> requestInfos, String errorCause) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFileDeleted(String checksum, String storage, String owner, Collection<RequestInfo> requestInfos) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFileUpdated(String checksum, String storage, FileReference updateFile) {
		// TODO Auto-generated method stub

	}

	@Override
	public ResponseEntity<InputStreamResource> downloadFile(String checksum) {
		if (!"checksumOk".equals(checksum)) {
			return new ResponseEntity<InputStreamResource>(HttpStatus.NOT_FOUND);
		}
		try {
			File file= new File("src/test/resources/result.json");
		    InputStream stream = new FileInputStream(file);
			return new ResponseEntity<InputStreamResource>(new InputStreamResource(stream), HttpStatus.OK);
		} catch (IOException e) {
			return new ResponseEntity<InputStreamResource>(HttpStatus.NOT_FOUND);
		}
	}

}
