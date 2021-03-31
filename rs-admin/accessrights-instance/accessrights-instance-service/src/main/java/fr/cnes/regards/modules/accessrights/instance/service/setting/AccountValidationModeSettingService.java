package fr.cnes.regards.modules.accessrights.instance.service.setting;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountValidation;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class AccountValidationModeSettingService {

    private final IDynamicTenantSettingService dynamicTenantSettingService;

    public AccountValidationModeSettingService(IDynamicTenantSettingService dynamicTenantSettingService) {
        this.dynamicTenantSettingService = dynamicTenantSettingService;
    }

    @EventListener
    public void init(ApplicationStartedEvent event) throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        try {
            dynamicTenantSettingService.read(AccountValidation.SETTING);
        } catch (EntityNotFoundException e) {
            DynamicTenantSetting dynamicTenantSetting = new DynamicTenantSetting();
            dynamicTenantSetting.setName(AccountValidation.SETTING);
            dynamicTenantSetting.setDescription("Accept Mode");
            dynamicTenantSetting.setDefaultValue(AccountValidation.DEFAULT_MODE.getName());
            dynamicTenantSetting.setValue(dynamicTenantSetting.getValue());
            dynamicTenantSettingService.create(dynamicTenantSetting);
        }
    }

    public AccountValidation.Mode currentMode() throws EntityNotFoundException {
        return AccountValidation.Mode.fromName(dynamicTenantSettingService.read(AccountValidation.SETTING).getValue());
    }

    public boolean isAutoAccept() {
        boolean isAutoAccept = false;
        try {
            isAutoAccept = AccountValidation.Mode.AUTO_ACCEPT.equals(currentMode());
        } catch (EntityNotFoundException e) {
            // do Nothing
        }
        return isAutoAccept;
    }

}
