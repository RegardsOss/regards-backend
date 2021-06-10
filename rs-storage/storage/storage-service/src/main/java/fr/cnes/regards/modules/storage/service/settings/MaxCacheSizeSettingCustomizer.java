package fr.cnes.regards.modules.storage.service.settings;

import java.util.Objects;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.storage.domain.StorageSetting;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class MaxCacheSizeSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        Object settingValue = dynamicTenantSetting.getValue();
        Object defaultSettingValue = dynamicTenantSetting.getDefaultValue();
        boolean valueIsValid = settingValue instanceof Long && (Long) settingValue > 0;
        boolean defaultValueIsValid = defaultSettingValue instanceof Long && (Long) defaultSettingValue > 0;
        return valueIsValid && defaultValueIsValid;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return Objects.equals(dynamicTenantSetting.getName(), StorageSetting.CACHE_MAX_SIZE_NAME);
    }
}
