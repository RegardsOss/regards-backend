package fr.cnes.regards.modules.storage.service.settings;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.domain.database.DefaultDownloadQuotaLimits;
import fr.cnes.regards.modules.storage.service.file.download.IQuotaService;
import io.vavr.control.Try;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class DefaultQuotaSettingCustomizer implements IDynamicTenantSettingCustomizer {

    @Autowired
    @Lazy
    private IQuotaService<?> quotaService;

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        Object settingValue = dynamicTenantSetting.getValue();
        Object defaultSettingValue = dynamicTenantSetting.getDefaultValue();
        boolean valueIsValid = settingValue != null && settingValue instanceof Long && (Long) settingValue > -2;
        boolean defaultValueIsValid = defaultSettingValue instanceof Long && (Long) defaultSettingValue > -2;
        return valueIsValid && defaultValueIsValid;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return Objects.equals(dynamicTenantSetting.getName(), StorageSetting.MAX_QUOTA_NAME);
    }

    @Override
    public void doRightNow(DynamicTenantSetting dynamicTenantSetting) {
        quotaService.changeDefaultQuotaLimits(dynamicTenantSetting.getValue());
    }
}
