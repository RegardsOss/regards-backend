package fr.cnes.regards.modules.search.rest.download;

import feign.Request;
import feign.Request.Body;
import feign.Response;
import fr.cnes.regards.modules.storage.client.FileReferenceEventDTO;
import fr.cnes.regards.modules.storage.client.FileReferenceUpdateDTO;
import fr.cnes.regards.modules.storage.client.IStorageFileListener;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Primary
@Service
public class IStorageRestClientMock implements IStorageRestClient, IStorageFileListener {

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
    public Response downloadFile(String checksum, boolean isContentInline) {
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

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(String userEmail) {
        return null;
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> upsertQuotaLimits(String userEmail, @Valid DownloadQuotaLimitsDto quotaLimits) {
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
