package fr.cnes.regards.modules.ingest.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.ingest.domain.settings.IngestSettings;
import org.springframework.stereotype.Component;

@Component
public class ActiveNotificationSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public ActiveNotificationSettingCustomizer() {
        super(IngestSettings.ACTIVE_NOTIFICATION,
              "parameter [active notification] can be null or must be a valid boolean");
    }

    @Override
    protected boolean isProperValue(Object value) {
        return value == null || value instanceof Boolean;
    }

}
