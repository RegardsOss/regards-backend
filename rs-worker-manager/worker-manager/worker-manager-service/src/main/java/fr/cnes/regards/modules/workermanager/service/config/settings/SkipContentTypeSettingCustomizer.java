package fr.cnes.regards.modules.workermanager.service.config.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerManagerSettings;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Dynamic tenant setting to configure a list of content type to skip
 *
 * @author Sébastien Binda
 */
@Component
public class SkipContentTypeSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        if (!isProperValue(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          "default setting value of parameter [skip content type] must be a list.");
        }
        if (!isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value", "setting value of parameter [skip content type] must be a list.");
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return Objects.equals(dynamicTenantSetting.getName(), WorkerManagerSettings.SKIP_CONTENT_TYPES_NAME);
    }

    private boolean isProperValue(Object value) {
        return value instanceof ArrayList;
    }
}
