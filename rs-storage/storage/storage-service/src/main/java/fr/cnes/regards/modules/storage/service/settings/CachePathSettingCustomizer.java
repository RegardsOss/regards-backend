package fr.cnes.regards.modules.storage.service.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.service.cache.CacheService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class CachePathSettingCustomizer implements IDynamicTenantSettingCustomizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachePathSettingCustomizer.class);

    @Autowired
    @Lazy
    private CacheService cacheService;

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        Object settingValue = dynamicTenantSetting.getValue();
        Object defaultSettingValue = dynamicTenantSetting.getDefaultValue();
        boolean valueIsValid = isValidPath((Path) settingValue);
        boolean defaultValueIsValid = isValidPath((Path) defaultSettingValue);
        return valueIsValid && defaultValueIsValid;
    }

    private boolean isValidPath(Path settingValue) {
        boolean isValid = false;
        if(settingValue != null) {
            try {
                Files.createDirectories(settingValue);
                isValid=true;
            } catch (IOException e) {
                LOGGER.error(String.format("Tenant cache path %s is invalid!", settingValue), e);
            }
        }
        return isValid;
    }

    @Override
    public boolean canBeModified(DynamicTenantSetting dynamicTenantSetting) {
        //Tenant cache path can only be modified if there is no CacheFile referenced at the moment
        return cacheService.isCacheEmpty();
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return Objects.equals(dynamicTenantSetting.getName(), StorageSetting.CACHE_PATH_NAME);
    }
}
