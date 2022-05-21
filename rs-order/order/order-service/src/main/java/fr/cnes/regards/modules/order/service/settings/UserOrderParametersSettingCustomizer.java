package fr.cnes.regards.modules.order.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.order.domain.settings.OrderSettings;
import fr.cnes.regards.modules.order.domain.settings.UserOrderParameters;
import org.springframework.stereotype.Component;

@Component
public class UserOrderParametersSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        return isProperValue(dynamicTenantSetting.getDefaultValue()) && isProperValue(dynamicTenantSetting.getValue());
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return OrderSettings.USER_ORDER_PARAMETERS.equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        boolean isProperValue;
        if (!(value instanceof UserOrderParameters)) {
            isProperValue = false;
        } else {
            int subOrderDuration = ((UserOrderParameters) value).getSubOrderDuration();
            int delayBeforeEmailNotification = ((UserOrderParameters) value).getDelayBeforeEmailNotification();
            isProperValue = delayBeforeEmailNotification > 0 && delayBeforeEmailNotification <= subOrderDuration;
        }
        return isProperValue;
    }

}
