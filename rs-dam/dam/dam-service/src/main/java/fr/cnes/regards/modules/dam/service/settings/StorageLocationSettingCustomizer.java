package fr.cnes.regards.modules.dam.service.settings;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.dam.domain.settings.DamSettings;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationDto;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class StorageLocationSettingCustomizer implements IDynamicTenantSettingCustomizer {

    private final IStorageRestClient storageRestClient;

    public StorageLocationSettingCustomizer(IStorageRestClient storageRestClient) {
        this.storageRestClient = storageRestClient;
    }

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        return isProperValue(dynamicTenantSetting.getDefaultValue()) && (dynamicTenantSetting.getValue() == null
                                                                         || isProperValue(dynamicTenantSetting.getValue()));
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return DamSettings.STORAGE_LOCATION.equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        return value instanceof String && (StringUtils.isEmpty(value) || isOnlineLocation((String) value));
    }

    private boolean isOnlineLocation(String location) {
        ResponseEntity<List<EntityModel<StorageLocationDto>>> responseEntity = storageRestClient.retrieve();
        if (!responseEntity.hasBody()) {
            return false;
        } else {
            List<StorageLocationDto> storageLocationList = HateoasUtils.unwrapList(responseEntity.getBody());
            return storageLocationList.stream()
                                      .filter(storageLocationDTO -> storageLocationDTO.getConfiguration()
                                                                                      .getStorageType()
                                                                                      .equals(StorageType.ONLINE))
                                      .anyMatch(storageLocationDTO -> storageLocationDTO.getName().equals(location));
        }
    }

}
