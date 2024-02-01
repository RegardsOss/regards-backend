package fr.cnes.regards.modules.accessrights.service.projectuser.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingCustomizer;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;

@Component
public class DefaultRoleSettingCustomizer implements IDynamicTenantSettingCustomizer {

    private final IRoleService roleService;

    public DefaultRoleSettingCustomizer(IRoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public Errors isValid(DynamicTenantSetting dynamicTenantSetting) {
        Errors errors = new MapBindingResult(new HashMap<>(), DynamicTenantSetting.class.getName());
        if (!roleService.existByName(dynamicTenantSetting.getDefaultValue())) {
            errors.reject("invalid.default.setting.value",
                          "default setting value of parameter [default role] must be a valid role.");
        }
        if (dynamicTenantSetting.getValue() != null && !roleService.existByName(dynamicTenantSetting.getValue())) {
            errors.reject("invalid.setting.value",
                          "setting value of parameter [default role] can be null or must be a valid role.");
        }
        return errors;
    }

    @Override
    public boolean appliesTo(DynamicTenantSetting dynamicTenantSetting) {
        return AccessSettings.DEFAULT_ROLE.equals(dynamicTenantSetting.getName());
    }

}
