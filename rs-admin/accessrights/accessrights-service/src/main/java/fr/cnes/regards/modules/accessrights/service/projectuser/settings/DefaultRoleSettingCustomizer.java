package fr.cnes.regards.modules.accessrights.service.projectuser.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import org.springframework.stereotype.Component;


@Component
public class DefaultRoleSettingCustomizer implements IDynamicTenantSettingCustomizer {

    private final IRoleService roleService;

    public DefaultRoleSettingCustomizer(IRoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public boolean isValid(DynamicTenantSetting dynamicTenantSetting) {
        return roleService.existByName(dynamicTenantSetting.getDefaultValue())
                && (dynamicTenantSetting.getValue() == null || roleService.existByName(dynamicTenantSetting.getValue()));
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return AccessSettings.DEFAULT_ROLE_SETTING.getName().equals(dynamicTenantSetting.getName());
    }

}
