package fr.cnes.regards.modules.storage.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.Objects;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class MaxCacheSizeSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        if (!isProperValue(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          "default setting value of parameter [max cache size] must be a valid positive number.");
        }
        if (!isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value",
                          "setting value of parameter [max cache size] must be a valid positive number.");
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return Objects.equals(dynamicTenantSetting.getName(), StorageSetting.CACHE_MAX_SIZE_NAME);
    }

    private boolean isProperValue(Object settingValue) {
        return settingValue instanceof Long maxCacheSize && maxCacheSize > 0;
    }
}
