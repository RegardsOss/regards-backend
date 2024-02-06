package fr.cnes.regards.modules.accessrights.service.projectuser.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.Set;

@Component
public class UserCreationMailRecipientsSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        if (!isProperValue(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          "default setting value of parameter [mail recipient] must be valid strings");
        }
        if (dynamicTenantSetting.getValue() != null && !isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value",
                          "setting value of parameter [mail recipient] can be null or must be valid strings");
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return AccessSettings.USER_CREATION_MAIL_RECIPIENTS.equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        return value instanceof Set && ((Set<?>) value).stream().allMatch(String.class::isInstance);
    }

}
