package fr.cnes.regards.modules.storage.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import org.springframework.stereotype.Component;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class MaxCacheSizeSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public MaxCacheSizeSettingCustomizer() {
        super(StorageSetting.CACHE_MAX_SIZE_NAME, "parameter [max cache size] must be a valid positive number");
    }

    @Override
    protected boolean isProperValue(Object settingValue) {
        return settingValue instanceof Long maxCacheSize && maxCacheSize > 0;
    }
}
