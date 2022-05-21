package fr.cnes.regards.modules.storage.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class DefaultRateSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Autowired
    @Lazy
    private IQuotaService<?> quotaService;

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        Object settingValue = dynamicTenantSetting.getValue();
        Object defaultSettingValue = dynamicTenantSetting.getDefaultValue();
        boolean valueIsValid = settingValue instanceof Long && (Long) settingValue > -2;
        boolean defaultValueIsValid = defaultSettingValue instanceof Long && (Long) defaultSettingValue > -2;
        return valueIsValid && defaultValueIsValid;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return Objects.equals(dynamicTenantSetting.getName(), StorageSetting.RATE_LIMIT_NAME);
    }

    @Override
    public void doRightNow(DynamicTenantSetting dynamicTenantSetting) {
        quotaService.changeDefaultRateLimits(dynamicTenantSetting.getValue());
    }
}
