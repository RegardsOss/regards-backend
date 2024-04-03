package fr.cnes.regards.modules.accessrights.service.projectuser.settings;

import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSimpleDynamicSettingCustomizer;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import org.springframework.stereotype.Component;

@Component
public class DefaultRoleSettingCustomizer extends AbstractSimpleDynamicSettingCustomizer {

    private final IRoleService roleService;

    public DefaultRoleSettingCustomizer(IRoleService roleService) {
        super(AccessSettings.DEFAULT_ROLE, "parameter [default role] can be null or must be a valid role");
        this.roleService = roleService;
    }

    @Override
    protected boolean isProperValue(Object settingValue) {
        if (settingValue == null) {
            return true;
        }
        // if a role is set in settingValue, make sure that this role exists
        if (settingValue instanceof String settingValueAsString) {
            return roleService.existByName(settingValueAsString);
        }
        return false;
    }
}
