package fr.cnes.regards.modules.dam.service.settings;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.dam.domain.settings.DamSettings;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.plugin.StorageType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.List;

@Component
public class StorageLocationSettingCustomizer implements IDynamicTenantSettingCustomizer {

    private final IStorageRestClient storageRestClient;

    public StorageLocationSettingCustomizer(IStorageRestClient storageRestClient) {
        this.storageRestClient = storageRestClient;
    }

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        if (!isProperValue(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          "default setting value of parameter [storage location] must be a valid string or existing online location.");
        }
        if (dynamicTenantSetting.getValue() != null && !isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value",
                          "setting value of parameter [storage location] can be null or must be a valid string or existing online location.");
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return DamSettings.STORAGE_LOCATION.equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        return value instanceof String storageLocation && (StringUtils.isBlank(storageLocation) || isOnlineLocation(
            storageLocation));
    }

    private boolean isOnlineLocation(String location) {
        ResponseEntity<List<EntityModel<StorageLocationDTO>>> responseEntity = storageRestClient.retrieve();
        if (!responseEntity.hasBody()) {
            return false;
        } else {
            List<StorageLocationDTO> storageLocationList = HateoasUtils.unwrapList(responseEntity.getBody());
            return storageLocationList.stream()
                                      .filter(storageLocationDTO -> storageLocationDTO.getConfiguration()
                                                                                      .getStorageType()
                                                                                      .equals(StorageType.ONLINE))
                                      .anyMatch(storageLocationDTO -> storageLocationDTO.getName().equals(location));
        }
    }

}
