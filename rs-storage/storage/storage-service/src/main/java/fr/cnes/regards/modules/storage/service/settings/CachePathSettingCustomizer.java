package fr.cnes.regards.modules.storage.service.settings;

import java.io.File;
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

    private boolean isValidPath(Path path) {
        boolean isValid = false;
        if (path != null) {
            // Check that the given path is available.
            File pathFile = path.toFile();
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                LOGGER.error(String.format("Tenant cache path %s is invalid!", path), e);
            }
            // In case pathFile does not represents a directory, Files#createDirectories will throw an exception that is already relayed to the user
            // So lets just handle case when it's a directory but rights are not right (in case the directory already existed)
            if (pathFile.isDirectory() && (!pathFile.canRead() || !pathFile.canWrite())) {
                LOGGER.error("Tenant cache path {} cannot be used by the application because of rights! "
                                     + "Ensure execution user can read and write in the directory.", path);
                isValid = true;
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
