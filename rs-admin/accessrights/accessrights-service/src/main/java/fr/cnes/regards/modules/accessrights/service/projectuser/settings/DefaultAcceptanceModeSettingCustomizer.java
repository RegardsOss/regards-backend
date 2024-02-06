package fr.cnes.regards.modules.accessrights.service.projectuser.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Component
public class DefaultAcceptanceModeSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        List<AccessSettings.AcceptanceMode> authorizedValues = Arrays.asList(AccessSettings.AcceptanceMode.values());
        if (!isProperValue(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          String.format(
                              "default setting value of parameter [default acceptance mode] must be a valid string in enum "
                              + "%s.",
                              authorizedValues));
        }
        if (dynamicTenantSetting.getValue() != null && !isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value",
                          String.format(
                              "setting value of parameter [default acceptance mode] can be null or must be a valid string "
                              + "in enum %s.",
                              authorizedValues));
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return AccessSettings.MODE.equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        return value instanceof String acceptanceMode && AccessSettings.AcceptanceMode.fromName(acceptanceMode) != null;
    }

}
