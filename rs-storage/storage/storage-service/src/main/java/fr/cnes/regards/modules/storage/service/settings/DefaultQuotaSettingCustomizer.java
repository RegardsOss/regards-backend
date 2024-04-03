package fr.cnes.regards.modules.storage.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class DefaultQuotaSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    @Autowired
    @Lazy
    private IQuotaService<?> quotaService;

    public DefaultQuotaSettingCustomizer() {
        super(StorageSetting.MAX_QUOTA_NAME, "parameter [quota] must be a valid number >= -1");
    }

    @Override
    public void doRightNow(DynamicTenantSetting dynamicTenantSetting) {
        quotaService.changeDefaultQuotaLimits(dynamicTenantSetting.getValue());
    }

    protected boolean isProperValue(Object settingValue) {
        return settingValue instanceof Long quota && quota >= -1;
    }
}
