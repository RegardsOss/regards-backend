package fr.cnes.regards.framework.modules.tenant.settings.service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    public void deleteAll() throws EntityNotFoundException {
        for (DynamicTenantSetting dynamicTenantSetting : getSettingList()) {
            dynamicTenantSettingService.delete(dynamicTenantSetting.getName());
        }
    }

    public Set<DynamicTenantSetting> retrieve() {
        return dynamicTenantSettingService.readAll(getSettingList().stream()
                                                                   .map(DynamicTenantSetting::getName)
                                                                   .collect(Collectors.toSet()));
    }

    public void resetSettings() throws EntityException {
        deleteAll();
        init();
    }

    public <T> T getValue(String name) {
        T value = null;
        Optional<DynamicTenantSetting> dynamicTenantSetting = dynamicTenantSettingService.read(name);
        if (dynamicTenantSetting.isPresent()) {
            value = dynamicTenantSetting.get().getValue();
        }
        return value;
    }

    private void createSetting(DynamicTenantSetting dynamicTenantSetting)
        throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        if (dynamicTenantSettingService.read(dynamicTenantSetting.getName()).isEmpty()) {
            dynamicTenantSettingService.create(dynamicTenantSetting);
        }
    }

}
