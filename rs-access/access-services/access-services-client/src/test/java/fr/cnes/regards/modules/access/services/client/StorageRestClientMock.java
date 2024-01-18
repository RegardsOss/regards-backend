package fr.cnes.regards.modules.access.services.client;

import feign.Response;
import fr.cnes.regards.modules.fileaccess.dto.availability.FileAvailabilityStatusDto;
import fr.cnes.regards.modules.fileaccess.dto.availability.FilesAvailabilityRequestDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationDto;
import fr.cnes.regards.modules.fileaccess.dto.quota.DownloadQuotaLimitsDto;
import fr.cnes.regards.modules.fileaccess.dto.quota.UserCurrentQuotasDto;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * Make Spring DI happy.
 */
@Primary
@Component
public class StorageRestClientMock implements IStorageRestClient {

    @Override
    public Response downloadFile(String checksum, Boolean isContentInline) {
        return null;
    }

    @Override
    public ResponseEntity<List<EntityModel<StorageLocationDto>>> retrieve() {
        return null;
    }

    @Override
    public Response export() {
        return null;
    }

    @Override
    public ResponseEntity<Set<FileReferenceDto>> getFileReferencesWithoutOwners(String storage, Set<String> checksums) {
        return null;
    }

    @Override
    public ResponseEntity<List<FileAvailabilityStatusDto>> checkFileAvailability(@Valid FilesAvailabilityRequestDto filesAvailabilityRequestDto) {
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
    public ResponseEntity<UserCurrentQuotasDto> getCurrentQuotas() {
        return null;
    }

    @Override
    public ResponseEntity<UserCurrentQuotasDto> getCurrentQuotas(String userEmail) {
        return null;
    }

    @Override
    public ResponseEntity<Long> getMaxQuota() {
        return null;
    }

    @Override
    public ResponseEntity<List<UserCurrentQuotasDto>> getCurrentQuotasList(String[] userEmails) {
        return null;
    }
}
