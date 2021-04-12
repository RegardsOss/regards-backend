package fr.cnes.regards.modules.storage.service.settings;

import java.util.Objects;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.storage.domain.StorageSettingName;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class DefaultQuotaSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        Object settingValue = dynamicTenantSetting.getValue();
        Object defaultSettingValue = dynamicTenantSetting.getDefaultValue();
        boolean valueIsValid = settingValue == null || (settingValue instanceof Long && (Long) settingValue > -1);
        boolean defaultValueIsValid = defaultSettingValue instanceof Long && (Long) defaultSettingValue > -1;
        return valueIsValid && defaultValueIsValid;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return Objects.equals(dynamicTenantSetting.getName(), StorageSettingName.MAX_QUOTA);
    }
}
