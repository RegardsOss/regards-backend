package fr.cnes.regards.modules.search.rest.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import feign.Response;
import fr.cnes.regards.modules.storagelight.client.IStorageFileListener;
import fr.cnes.regards.modules.storagelight.client.IStorageRestClient;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.dto.StorageLocationDTO;

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
    public Response downloadFile(String checksum) {
        if (!"checksumOk".equals(checksum)) {
            return Response.builder().status(HttpStatus.NOT_FOUND.value()).build();
        }
        try {
            File file = new File("src/test/resources/result.json");
            InputStream stream = new FileInputStream(file);
            return Response.builder().status(HttpStatus.OK.value()).body(stream, 150).build();
        } catch (IOException e) {
            return Response.builder().status(HttpStatus.NOT_FOUND.value()).build();
        }
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.storagelight.client.IStorageRestClient#export()
     */
    @Override
    public Response export() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.storagelight.client.IStorageRestClient#retrieve()
     */
    @Override
    public ResponseEntity<List<Resource<StorageLocationDTO>>> retrieve() {
        // TODO Auto-generated method stub
        return null;
    }

}
