package fr.cnes.regards.modules.feature.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.feature.domain.settings.FeatureNotificationSettings;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;

@Component
public class ActiveNotificationSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        if (!isProperValue(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          "default setting value of parameter [active notification] must be a valid boolean.");
        }
        if (dynamicTenantSetting.getValue() != null && !isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value",
                          "setting value of parameter [active notification] can be null or must be a valid boolean.");
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return FeatureNotificationSettings.ACTIVE_NOTIFICATION.equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        return value instanceof Boolean;
    }

}
