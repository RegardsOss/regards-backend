package fr.cnes.regards.modules.order.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import org.springframework.stereotype.Component;

@Component
public class AppSubOrderDurationSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public AppSubOrderDurationSettingCustomizer() {
        super(OrderSettings.APP_SUB_ORDER_DURATION, "parameter [suborder creation] must be a valid positive number");
    }

    @Override
    protected boolean isProperValue(Object value) {
        return value instanceof Integer subOrderDuration && subOrderDuration >= 0;
    }
}
