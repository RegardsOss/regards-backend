package fr.cnes.regards.modules.workermanager.service.config.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerManagerSettings;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Dynamic tenant setting to configure a list of content type to skip
 *
 * @author Sébastien Binda
 */
@Component
public class SkipContentTypeSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public SkipContentTypeSettingCustomizer() {
        super(WorkerManagerSettings.SKIP_CONTENT_TYPES_NAME, "parameter [skip content type] must be a list");
    }

    @Override
    protected boolean isProperValue(Object value) {
        return value instanceof ArrayList;
    }
}
