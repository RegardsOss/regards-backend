package fr.cnes.regards.modules.workermanager.service.config.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerManagerSettings;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Dynamic tenant setting to configure a list of content type to skip
 *
 * @author Sébastien Binda
 */
@Component
public class SkipContentTypeSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        return isProperValue(dynamicTenantSetting.getDefaultValue()) && isProperValue(dynamicTenantSetting.getValue());
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return Objects.equals(dynamicTenantSetting.getName(), WorkerManagerSettings.SKIP_CONTENT_TYPES_NAME);
    }

    private boolean isProperValue(Object value) {
        return value instanceof ArrayList;
    }
}
