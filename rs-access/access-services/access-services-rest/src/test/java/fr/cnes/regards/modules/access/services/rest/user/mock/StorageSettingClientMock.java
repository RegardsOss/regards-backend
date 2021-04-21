package fr.cnes.regards.modules.access.services.rest.user.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.tenant.settings.client.IDynamicTenantSettingClient;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import fr.cnes.regards.modules.storage.domain.StorageSetting;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class StorageSettingClientMock implements IStorageSettingClient {

    private static final Random random = new Random();

    public static final Long DEFAULT_QUOTA_LIMITS_STUB_MAX_QUOTA = (long) random.nextInt(10_000);

    public static final Long DEFAULT_QUOTA_LIMITS_STUB_RATE_LIMIT = (long) random.nextInt(10_000);

    public static final List<DynamicTenantSetting> DEFAULT_STORAGE_SETTING_STUB = Arrays
            .asList(StorageSetting.MAX_QUOTA.setValue(DEFAULT_QUOTA_LIMITS_STUB_MAX_QUOTA),
                    StorageSetting.RATE_LIMIT.setValue(DEFAULT_QUOTA_LIMITS_STUB_RATE_LIMIT));


    @Override
    public ResponseEntity<List<EntityModel<DynamicTenantSetting>>> retrieveAll(Set<String> names) {
        return new ResponseEntity<>(HateoasUtils.wrapList(DEFAULT_STORAGE_SETTING_STUB), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<EntityModel<DynamicTenantSetting>> update(String name, DynamicTenantSetting setting) {
        return new ResponseEntity<>(new EntityModel<>(setting), HttpStatus.OK);
    }

}
