package fr.cnes.regards.modules.ingest.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.ingest.domain.settings.IngestSettings;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;

@Component
public class SipBodyTimeToLiveSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        if (!isProperValue(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          "default setting value of parameter [sip body time to live] must be a valid positive number"
                          + " or -1 or disable expiration of sip body.");
        }
        if (dynamicTenantSetting.getValue() != null && !isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value",
                          "setting value of parameter [sip body time to live] can be null or must be a valid positive"
                          + " number or -1 or disable expiration of sip body.");
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return IngestSettings.SIP_BODY_TIME_TO_LIVE.equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        return value instanceof Integer sipBodyTtl && sipBodyTtl >= -1;
    }

}
