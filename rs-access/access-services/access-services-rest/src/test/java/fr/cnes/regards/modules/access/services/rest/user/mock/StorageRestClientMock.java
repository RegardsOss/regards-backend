package fr.cnes.regards.modules.access.services.rest.user.mock;

import feign.Response;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.modules.filecatalog.dto.FileReferenceDto;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import fr.cnes.regards.modules.filecatalog.dto.availability.FileAvailabilityStatusDto;
import fr.cnes.regards.modules.filecatalog.dto.files.FilesAvailabilityRequestDto;
import fr.cnes.regards.modules.filecatalog.dto.quota.DownloadQuotaLimitsDto;
import fr.cnes.regards.modules.filecatalog.dto.quota.UserCurrentQuotasDto;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Primary
@Component
public class StorageRestClientMock implements IStorageRestClient {

    public static final String USER_QUOTA_LIMITS_STUB_EMAIL = UUID.randomUUID().toString();

    private static final Random random = new Random();

    public static final long USER_QUOTA_LIMITS_STUB_MAX_QUOTA = random.nextInt(10_000);

    public static final long USER_QUOTA_LIMITS_STUB_RATE_LIMIT = random.nextInt(10_000);

    public static final DownloadQuotaLimitsDto USER_QUOTA_LIMITS_STUB = new DownloadQuotaLimitsDto(
        USER_QUOTA_LIMITS_STUB_EMAIL,
        USER_QUOTA_LIMITS_STUB_MAX_QUOTA,
        USER_QUOTA_LIMITS_STUB_RATE_LIMIT);

    public static final long CURRENT_USER_QUOTA_STUB = random.nextInt(10_000);

    public static final long CURRENT_USER_RATE_STUB = random.nextInt(10_000);

    @Autowired
    private IAuthenticationResolver authResolver;

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits(String userEmail) {
        return new ResponseEntity<>(USER_QUOTA_LIMITS_STUB, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> upsertQuotaLimits(String userEmail,
                                                                    @Valid DownloadQuotaLimitsDto quotaLimits) {
        return new ResponseEntity<>(quotaLimits, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<DownloadQuotaLimitsDto>> getQuotaLimits(String[] userEmails) {
        return new ResponseEntity<>(Arrays.stream(userEmails)
                                          .map(userEmail -> new DownloadQuotaLimitsDto(userEmail,
                                                                                       USER_QUOTA_LIMITS_STUB_MAX_QUOTA,
                                                                                       USER_QUOTA_LIMITS_STUB_RATE_LIMIT))
                                          .collect(Collectors.toList()), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DownloadQuotaLimitsDto> getQuotaLimits() {
        return new ResponseEntity<>(new DownloadQuotaLimitsDto(authResolver.getUser(),
                                                               USER_QUOTA_LIMITS_STUB_MAX_QUOTA,
                                                               USER_QUOTA_LIMITS_STUB_RATE_LIMIT), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<UserCurrentQuotasDto> getCurrentQuotas() {
        return new ResponseEntity<>(new UserCurrentQuotasDto(authResolver.getUser(),
                                                             USER_QUOTA_LIMITS_STUB_MAX_QUOTA,
                                                             USER_QUOTA_LIMITS_STUB_RATE_LIMIT,
                                                             CURRENT_USER_QUOTA_STUB,
                                                             CURRENT_USER_RATE_STUB), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<UserCurrentQuotasDto> getCurrentQuotas(String userEmail) {
        return new ResponseEntity<>(new UserCurrentQuotasDto(authResolver.getUser(),
                                                             USER_QUOTA_LIMITS_STUB_MAX_QUOTA,
                                                             USER_QUOTA_LIMITS_STUB_RATE_LIMIT,
                                                             CURRENT_USER_QUOTA_STUB,
                                                             CURRENT_USER_RATE_STUB), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Long> getMaxQuota() {
        return new ResponseEntity<>(10_000L, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<UserCurrentQuotasDto>> getCurrentQuotasList(String[] userEmails) {
        return new ResponseEntity<>(Arrays.stream(userEmails)
                                          .map(userEmail -> new UserCurrentQuotasDto(userEmail,
                                                                                     USER_QUOTA_LIMITS_STUB_MAX_QUOTA,
                                                                                     USER_QUOTA_LIMITS_STUB_RATE_LIMIT,
                                                                                     CURRENT_USER_QUOTA_STUB,
                                                                                     CURRENT_USER_RATE_STUB))
                                          .collect(Collectors.toList()), HttpStatus.OK);
    }

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

}
