package fr.cnes.regards.modules.access.services.rest.user.mock;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import feign.Response;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.domain.database.UserCurrentQuotas;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;

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
        return new ResponseEntity<>(Arrays.stream(userEmails).map(userEmail -> new DownloadQuotaLimitsDto(userEmail,
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
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas() {
        return new ResponseEntity<>(new UserCurrentQuotas(authResolver.getUser(),
                                                          USER_QUOTA_LIMITS_STUB_MAX_QUOTA,
                                                          USER_QUOTA_LIMITS_STUB_RATE_LIMIT,
                                                          CURRENT_USER_QUOTA_STUB,
                                                          CURRENT_USER_RATE_STUB), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<UserCurrentQuotas> getCurrentQuotas(String userEmail) {
        return new ResponseEntity<>(new UserCurrentQuotas(authResolver.getUser(),
                                                          USER_QUOTA_LIMITS_STUB_MAX_QUOTA,
                                                          USER_QUOTA_LIMITS_STUB_RATE_LIMIT,
                                                          CURRENT_USER_QUOTA_STUB,
                                                          CURRENT_USER_RATE_STUB), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<UserCurrentQuotas>> getCurrentQuotasList(String[] userEmails) {
        return new ResponseEntity<>(Arrays.stream(userEmails).map(userEmail -> new UserCurrentQuotas(userEmail,
                                                                                                     USER_QUOTA_LIMITS_STUB_MAX_QUOTA,
                                                                                                     USER_QUOTA_LIMITS_STUB_RATE_LIMIT,
                                                                                                     CURRENT_USER_QUOTA_STUB,
                                                                                                     CURRENT_USER_RATE_STUB))
                                            .collect(Collectors.toList()), HttpStatus.OK);
    }

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

}
