package fr.cnes.regards.modules.ingest.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.ingest.domain.settings.AIPNotificationSettings;
import org.springframework.stereotype.Component;


@Component
public class ActiveNotificationSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        return dynamicTenantSetting.getDefaultValue() != null
                && isProperValue(dynamicTenantSetting.getDefaultValue())
                && (dynamicTenantSetting.getValue() == null || isProperValue(dynamicTenantSetting.getValue()));
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return AIPNotificationSettings.ACTIVE_NOTIFICATION.equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        return value instanceof Boolean;
    }

}
