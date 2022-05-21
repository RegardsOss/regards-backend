package fr.cnes.regards.modules.accessrights.service.projectuser;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSettingDto;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class QuotaHelperService {

    private static final Logger LOG = LoggerFactory.getLogger(QuotaHelperService.class);

    private static final String MAX_QUOTA_SETTING = StorageSetting.MAX_QUOTA_NAME;

    private static final Long MAX_QUOTA_DEFAULT_VALUE = -1L;

    private final IStorageSettingClient storageSettingClient;

    public QuotaHelperService(IStorageSettingClient storageSettingClient) {
        this.storageSettingClient = storageSettingClient;
    }

    public Long getDefaultQuota() {

        Long defaultQuota = MAX_QUOTA_DEFAULT_VALUE;

        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<List<EntityModel<DynamicTenantSettingDto>>> response = storageSettingClient.retrieveAll(
                Collections.singleton(MAX_QUOTA_SETTING));
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                defaultQuota = ((DynamicTenantSettingDto<Double>) HateoasUtils.unwrapCollection(response.getBody())
                                                                              .get(0)).getValue().longValue();
            }
        } catch (Exception e) {
            LOG.warn("Unable to retrieve default quota value from storage service - using default value", e);
        } finally {
            FeignSecurityManager.reset();
        }

        return defaultQuota;
    }

}
