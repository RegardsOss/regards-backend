package fr.cnes.regards.modules.storage.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
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
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        if (!isProperValue(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          "default setting value of parameter [rate] must be a valid number >= -1.");
        }
        if (!isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value", "setting value of parameter [rate] must be a valid number >= -1.");
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return Objects.equals(dynamicTenantSetting.getName(), StorageSetting.RATE_LIMIT_NAME);
    }

    @Override
    public void doRightNow(DynamicTenantSetting dynamicTenantSetting) {
        quotaService.changeDefaultRateLimits(dynamicTenantSetting.getValue());
    }

    private boolean isProperValue(Object settingValue) {
        return settingValue instanceof Long rate && rate >= -1;
    }
}
