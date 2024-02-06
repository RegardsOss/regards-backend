package fr.cnes.regards.modules.accessrights.service.projectuser.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.List;

@Component
public class DefaultGroupsSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        if (!isProperValue(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          "default setting value of parameter [default groups] must be valid strings");
        }
        if (dynamicTenantSetting.getValue() != null && !isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value",
                          "setting value of parameter [default groups] can be null or must be valid strings");
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return AccessSettings.DEFAULT_GROUPS.equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        return value instanceof List && ((List<?>) value).stream().allMatch(String.class::isInstance);
    }

}
