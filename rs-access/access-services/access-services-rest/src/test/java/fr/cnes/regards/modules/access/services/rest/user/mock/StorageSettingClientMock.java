package fr.cnes.regards.modules.access.services.rest.user.mock;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSettingDto;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import org.springframework.context.annotation.Primary;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Primary
@Component
public class StorageSettingClientMock implements IStorageSettingClient {

    private static final Random random = new Random();

    public static final Long DEFAULT_QUOTA_LIMITS_STUB_MAX_QUOTA = (long) random.nextInt(10_000);

    public static final Long DEFAULT_QUOTA_LIMITS_STUB_RATE_LIMIT = (long) random.nextInt(10_000);

    @Override
    public ResponseEntity<List<EntityModel<DynamicTenantSettingDto>>> retrieveAll(Set<String> names) {
        List<DynamicTenantSettingDto> DEFAULT_STORAGE_SETTING_STUB = Arrays
                .asList(StorageSetting.MAX_QUOTA.setValue(DEFAULT_QUOTA_LIMITS_STUB_MAX_QUOTA),
                        StorageSetting.RATE_LIMIT.setValue(DEFAULT_QUOTA_LIMITS_STUB_RATE_LIMIT)).stream()
                .map(DynamicTenantSettingDto::new).collect(Collectors.toList());
        return new ResponseEntity<>(HateoasUtils.wrapList(DEFAULT_STORAGE_SETTING_STUB), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EntityModel<DynamicTenantSettingDto>> update(String name, DynamicTenantSettingDto setting) {
        return new ResponseEntity<>(EntityModel.of(setting), HttpStatus.OK);
    }

}
