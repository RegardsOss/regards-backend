package fr.cnes.regards.modules.dam.service.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.dam.domain.settings.DamSettings;
import org.springframework.stereotype.Component;

@Component
public class IndexNumberOfReplicasSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public IndexNumberOfReplicasSettingCustomizer() {
        super(DamSettings.INDEX_NUMBER_OF_REPLICAS,
              "parameter [number of replicas] can be null or must be a valid number");
    }

    @Override
    protected boolean isProperValue(Object value) {
        return value == null || value instanceof Long;
    }

}

