package fr.cnes.regards.modules.dam.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.dam.domain.settings.DamSettings;
import org.springframework.stereotype.Component;

@Component
public class StoreFilesSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public StoreFilesSettingCustomizer() {
        super(DamSettings.STORE_FILES, "parameter [store files] can be null or must be a valid boolean");
    }

    @Override
    protected boolean isProperValue(Object value) {
        return value == null || value instanceof Boolean;
    }

}
