package fr.cnes.regards.modules.accessrights.service.projectuser.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultGroupsSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    public DefaultGroupsSettingCustomizer() {
        super(AccessSettings.DEFAULT_GROUPS, "parameter [default groups] can be null or must be valid strings");
    }

    @Override
    protected boolean isProperValue(Object value) {
        return value == null || value instanceof List && ((List<?>) value).stream().allMatch(String.class::isInstance);
    }

}
