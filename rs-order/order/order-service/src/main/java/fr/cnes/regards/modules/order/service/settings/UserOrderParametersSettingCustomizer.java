package fr.cnes.regards.modules.order.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import org.springframework.stereotype.Component;

@Component
public class UserOrderParametersSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public UserOrderParametersSettingCustomizer() {
        super(OrderSettings.USER_ORDER_PARAMETERS,
              "parameter [user order parameters] must have a valid positive delayBeforeEmailNotification <= subOrderDuration.");
    }

    @Override
    protected boolean isProperValue(Object value) {
        boolean isProperValue = false;
        if (value instanceof UserOrderParameters userOrderParameters) {
            int subOrderDuration = userOrderParameters.getSubOrderDuration();
            int delayBeforeEmailNotification = userOrderParameters.getDelayBeforeEmailNotification();
            isProperValue = delayBeforeEmailNotification > 0 && delayBeforeEmailNotification <= subOrderDuration;
        }
        return isProperValue;
    }

}
