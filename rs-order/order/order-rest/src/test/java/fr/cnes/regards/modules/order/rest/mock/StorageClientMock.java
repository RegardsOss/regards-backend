package fr.cnes.regards.modules.order.rest.mock;

import feign.Request;
import feign.Response;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Primary
public class StorageClientMock implements IStorageRestClient {

    public static final String NO_QUOTA_MSG_STUB = "No quota to download this file";

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClientMock.class);

    @Override
    public Response downloadFile(String checksum) {
        Map<String, Collection<String>> map = new HashMap<>();
        Request request = Request.create(Request.HttpMethod.GET, "test", map, Request.Body.empty());
        return Response.builder()
            .status(HttpStatus.TOO_MANY_REQUESTS.value())
            .body(NO_QUOTA_MSG_STUB, StandardCharsets.UTF_8)
            .request(request)
            .build();
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
