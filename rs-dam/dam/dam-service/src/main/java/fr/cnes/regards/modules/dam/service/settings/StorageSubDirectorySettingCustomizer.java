package fr.cnes.regards.modules.dam.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.dam.domain.settings.DamSettings;
import org.springframework.stereotype.Component;

@Component
public class StorageSubDirectorySettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public StorageSubDirectorySettingCustomizer() {
        super(DamSettings.STORAGE_SUBDIRECTORY,
              "parameter [storage subdirectory] can be null or must be a valid string");
    }

    @Override
    protected boolean isProperValue(Object value) {
        return value == null || value instanceof String;
    }

}
