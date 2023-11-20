package fr.cnes.regards.modules.search.rest.download;

import feign.Request;
import feign.Request.Body;
import feign.RequestTemplate;
import feign.Response;
import fr.cnes.regards.framework.security.autoconfigure.CustomCacheControlHeadersWriter;
import fr.cnes.regards.modules.search.rest.FakeFileFactory;
import fr.cnes.regards.modules.storage.client.FileReferenceEventDTO;
import fr.cnes.regards.modules.storage.client.FileReferenceUpdateDTO;
import fr.cnes.regards.modules.storage.client.IStorageFileListener;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceDTO;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import org.mockito.ArgumentMatchers;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import javax.annotation.Nullable;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Primary
@Service
public class IStorageRestClientMock implements IStorageRestClient, IStorageFileListener {

    private final IStorageRestClient storageClient;

    public static final String STORAGE_DEFAULT_DOWNLOAD_FILE_NAME = "storage_file_name.dat";

    private FakeFileFactory files;

    public IStorageRestClientMock() {
        storageClient = mock(IStorageRestClient.class);
    }

    public void setup(StorageDownloadStatus downloadStatus) {
        files = new FakeFileFactory();
        mockFileDownload(downloadStatus);
    }

    private void mockFileDownload(StorageDownloadStatus downloadStatus) {
        if (downloadStatus == StorageDownloadStatus.HTTP_ERROR) {
            when(storageClient.downloadFile(validFiles(),
                                            any())).thenThrow(new HttpServerErrorException(HttpStatus.BAD_REQUEST,
                                                                                           "http error"));
        } else if (downloadStatus == StorageDownloadStatus.FAILURE) {
            when(storageClient.downloadFile(validFiles(), any())).thenReturn(storageResponse(HttpStatus.NOT_FOUND,
                                                                                             "errors.",
                                                                                             null));
        } else {
            // Can't inline because response is mocked (can't mock during mock creation)
            Response invalidResponse = invalidStorageResponse();
            when(storageClient.downloadFile(eq(files.invalidFile()), any())).thenReturn(invalidResponse);
            when(storageClient.downloadFile(validFiles(), any())).thenReturn(storageResponse(HttpStatus.OK,
                                                                                             "content",
                                                                                             donwloadFileDefaultHeaders()));

        }
    }

    /**
     * Simulate default download file headers from storage response.
     */
    private HashMap<String, Collection<String>> donwloadFileDefaultHeaders() {
        HashMap<String, Collection<String>> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_DISPOSITION,
                    Collections.singletonList(ContentDisposition.builder("attachment")
                                                                .filename("storage_file_name.dat")
                                                                .build()
                                                                .toString()));
        return headers;
    }

    private String validFiles() {
        return ArgumentMatchers.argThat(s -> files.validFiles().contains(s));
    }

    private Response storageResponse(HttpStatus status,
                                     String fileContent,
                                     @Nullable Map<String, Collection<String>> addionalHeaders) {
        return Response.builder()
                       .status(status.value())
                       .headers(headers(addionalHeaders))
                       .body(fileContent.getBytes())
                       .request(request())
                       .build();
    }

    private Response invalidStorageResponse() {
        return Response.builder()
                       .status(HttpStatus.OK.value())
                       .headers(headers(null))
                       .body(body())
                       .request(request())
                       .build();
    }

    private Map<String, Collection<String>> headers(@Nullable Map<String, Collection<String>> additionalHeaders) {
        HashMap<String, Collection<String>> headers = new HashMap<>();
        headers.put(CustomCacheControlHeadersWriter.CACHE_CONTROL, Collections.singletonList("cache"));
        headers.put(CustomCacheControlHeadersWriter.EXPIRES, Collections.singletonList("expires"));
        headers.put(CustomCacheControlHeadersWriter.PRAGMA, Collections.singletonList("pragma"));
        headers.put("key1", Arrays.asList("value1", "value2"));
        if (additionalHeaders != null) {
            headers.putAll(additionalHeaders);
        }
        return headers;
    }

    private Response.Body body() {
        try {
            Response.Body body = mock(Response.Body.class);
            when(body.asInputStream()).thenThrow(new IOException("file pb"));
            return body;
        } catch (IOException e) {
            throw new RuntimeException("Error while mocking storage mock", e);
        }
    }

    private Request request() {
        return Request.create(Request.HttpMethod.GET, "url", headers(null), Body.empty(), new RequestTemplate());
    }

    @Override
    public void onFileStored(List<FileReferenceEventDTO> stored) {

    }

    @Override
    public void onFileStoreError(List<FileReferenceEventDTO> storedError) {

    }

    @Override
    public void onFileAvailable(List<FileReferenceEventDTO> available) {

    }

    @Override
    public void onFileNotAvailable(List<FileReferenceEventDTO> availabilityError) {

    }

    @Override
    public void onFileDeletedForOwner(String owner, List<FileReferenceEventDTO> deletedForThisOwner) {

    }

    @Override
    public void onFileUpdated(List<FileReferenceUpdateDTO> updatedReferences) {

    }

    @Override
    public Response downloadFile(String checksum, Boolean isContentInline) {
        return storageClient.downloadFile(checksum, isContentInline);
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.storage.client.IStorageRestClient#export()
     */
    @Override
    public Response export() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResponseEntity<Set<FileReferenceDTO>> getFileReferencesWithoutOwners(String storage, Set<String> checksums) {
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

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(String userEmail) {
        return null;
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> upsertQuotaLimits(String userEmail,
                                                                    @Valid DownloadQuotaLimitsDto quotaLimits) {
        return null;
    }

    @Override
    public ResponseEntity<List<DownloadQuotaLimitsDto>> getQuotaLimits(String[] userEmails) {
        return null;
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits() {
        return null;
    }

    @Override
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas() {
        return null;
    }

    @Override
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas(String userEmail) {
        return null;
    }

    @Override
    public ResponseEntity<List<UserCurrentQuotas>> getCurrentQuotasList(String[] userEmails) {
        return null;
    }
}
