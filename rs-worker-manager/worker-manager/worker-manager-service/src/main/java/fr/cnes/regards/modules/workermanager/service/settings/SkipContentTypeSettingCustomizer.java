package fr.cnes.regards.modules.workermanager.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.workermanager.domain.WorkerManagerSetting;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Objects;

@Component
public class SkipContentTypeSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        return isProperValue(dynamicTenantSetting.getDefaultValue()) && isProperValue(dynamicTenantSetting.getValue());
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return Objects.equals(dynamicTenantSetting.getName(), WorkerManagerSetting.SKIP_CONTENT_TYPES_NAME);
    }

    private boolean isProperValue(Object value) {
        return value instanceof ArrayList;
    }
}
