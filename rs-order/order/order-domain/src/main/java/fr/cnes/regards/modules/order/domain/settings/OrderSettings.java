package fr.cnes.regards.modules.order.domain.settings;

import com.google.common.collect.ImmutableList;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.util.List;

public final class OrderSettings {

    private OrderSettings() {
    }

    public static final String USER_ORDER_PARAMETERS = "user_order_parameters";
    public static final String APP_SUB_ORDER_DURATION = "app_sub_order_duration";

    public static final int DEFAULT_USER_SUB_ORDER_DURATION = 240;
    public static final int DEFAULT_DELAY_BEFORE_EMAIL_NOTIFICATION = 72;
    public static final int DEFAULT_APP_SUB_ORDER_DURATION = 2;

    public static final DynamicTenantSetting USER_ORDER_PARAMETERS_SETTING = new DynamicTenantSetting(
            USER_ORDER_PARAMETERS,
            "Parameters for User order (in hours)",
            new UserOrderParameters(DEFAULT_USER_SUB_ORDER_DURATION, DEFAULT_DELAY_BEFORE_EMAIL_NOTIFICATION)
    );
    public static final DynamicTenantSetting APP_SUB_ORDER_DURATION_SETTING = new DynamicTenantSetting(
            APP_SUB_ORDER_DURATION,
            "Sub order duration for applicative orders (in hours)",
            DEFAULT_APP_SUB_ORDER_DURATION
    );

    public static final List<DynamicTenantSetting> SETTING_LIST = ImmutableList.of(
            USER_ORDER_PARAMETERS_SETTING,
            APP_SUB_ORDER_DURATION_SETTING
    );

}
