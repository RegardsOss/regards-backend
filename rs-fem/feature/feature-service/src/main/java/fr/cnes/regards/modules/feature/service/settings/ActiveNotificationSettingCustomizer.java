package fr.cnes.regards.modules.feature.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.feature.domain.settings.FeatureNotificationSettings;
import org.springframework.stereotype.Component;

@Component
public class ActiveNotificationSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public ActiveNotificationSettingCustomizer() {
        super(FeatureNotificationSettings.ACTIVE_NOTIFICATION,
              "parameter [active notification]  can be null or must be a valid boolean");
    }

    @Override
    protected boolean isProperValue(Object settingValue) {
        return settingValue == null || settingValue instanceof Boolean;
    }
}
