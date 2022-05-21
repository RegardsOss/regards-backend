package fr.cnes.regards.modules.order.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.order.domain.settings.OrderSettings;
import org.springframework.stereotype.Component;

@Component
public class AppSubOrderDurationSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        return isProperValue(dynamicTenantSetting.getDefaultValue()) && isProperValue(dynamicTenantSetting.getValue());
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return OrderSettings.APP_SUB_ORDER_DURATION.equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        return value instanceof Integer && (Integer) value >= 0;
    }

}
