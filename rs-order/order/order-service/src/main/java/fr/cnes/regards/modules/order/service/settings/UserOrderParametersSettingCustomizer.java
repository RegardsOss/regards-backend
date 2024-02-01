package fr.cnes.regards.modules.order.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.order.domain.settings.OrderSettings;
import fr.cnes.regards.modules.order.domain.settings.UserOrderParameters;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;

@Component
public class UserOrderParametersSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        if (!isProperValue(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          "default setting value of parameter [user order parameters] must have a valid "
                          + "positive delayBeforeEmailNotification <= subOrderDuration.");
        }
        if (!isProperValue(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value",
                          "setting value of parameter [user order parameters] must have a valid "
                          + "positive delayBeforeEmailNotification <= subOrderDuration.");
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return OrderSettings.USER_ORDER_PARAMETERS.equals(dynamicTenantSetting.getName());
    }

    private boolean isProperValue(Object value) {
        boolean isProperValue = false;
        if (value instanceof UserOrderParameters userOrderParameters) {
            int subOrderDuration = userOrderParameters.getSubOrderDuration();
            int delayBeforeEmailNotification = userOrderParameters.getDelayBeforeEmailNotification();
            isProperValue = delayBeforeEmailNotification > 0 && delayBeforeEmailNotification <= subOrderDuration;
        }
        return isProperValue;
    }

}
