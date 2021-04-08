package fr.cnes.regards.framework.modules.tenant.settings.service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.util.List;

@RegardsTransactional
public abstract class AbstractSettingService {

    protected final IDynamicTenantSettingService dynamicTenantSettingService;

    protected AbstractSettingService(IDynamicTenantSettingService dynamicTenantSettingService) {
        this.dynamicTenantSettingService = dynamicTenantSettingService;
    }

    protected abstract List<DynamicTenantSetting> getSettingList();

    public void init() throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        for (DynamicTenantSetting dynamicTenantSetting : getSettingList()) {
            createSetting(dynamicTenantSetting);
        }
    }

    private void createSetting(DynamicTenantSetting dynamicTenantSetting) throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        if (!dynamicTenantSettingService.read(dynamicTenantSetting.getName()).isPresent()) {
            dynamicTenantSettingService.create(dynamicTenantSetting);
        }
    }

}
