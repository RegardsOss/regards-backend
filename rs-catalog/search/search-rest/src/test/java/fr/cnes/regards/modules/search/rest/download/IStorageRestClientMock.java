package fr.cnes.regards.modules.search.rest.download;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import feign.Request;
import feign.Request.Body;
import feign.Response;
import fr.cnes.regards.modules.storage.client.IStorageFileListener;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;

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
        Map<String, Collection<String>> map = new HashMap<>();
        Request request = Request.create(Request.HttpMethod.GET, "test", map, Body.empty());
        if (!"checksumOk".equals(checksum)) {
            return Response.builder().status(HttpStatus.NOT_FOUND.value()).reason("not found").request(request)
                    .headers(map).build();
        }
        return Response.builder().status(HttpStatus.OK.value()).body("result file content", Charset.defaultCharset())
                .request(request).headers(map).build();
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.storage.client.IStorageRestClient#export()
     */
    @Override
    public Response export() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.storage.client.IStorageRestClient#retrieve()
     */
    @Override
    public ResponseEntity<List<EntityModel<StorageLocationDTO>>> retrieve() {
        // TODO Auto-generated method stub
        return null;
    }

}
