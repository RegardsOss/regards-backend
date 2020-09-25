package fr.cnes.regards.modules.access.services.client;

import feign.Response;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.util.List;

/**
 * Make Spring DI happy.
 */
@Primary
@Component
public class StorageRestClientMock implements IStorageRestClient {
    @Override
    public Response downloadFile(String checksum) {
        return null;
    }

    @Override
    public ResponseEntity<List<EntityModel<StorageLocationDTO>>> retrieve() {
        return null;
    }

    @Override
    public Response export() {
        return null;
    }

    @Override
    public ResponseEntity<DefaultDownloadQuotaLimits> getDefaultDownloadQuotaLimits() {
        return null;
    }

    @Override
    public ResponseEntity<DefaultDownloadQuotaLimits> changeDefaultDownloadQuotaLimits(@Valid DefaultDownloadQuotaLimits newDefaults) {
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
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits() {
        return null;
    }

    @Override
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas() {
        return null;
    }
}
