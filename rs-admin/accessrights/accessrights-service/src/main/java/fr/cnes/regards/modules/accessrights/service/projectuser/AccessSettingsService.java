/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.service.projectuser;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingService;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class AccessSettingsService {

    private final DynamicTenantSettingService dynamicTenantSettingService;

    public AccessSettingsService(DynamicTenantSettingService dynamicTenantSettingService) {
        this.dynamicTenantSettingService = dynamicTenantSettingService;
    }

    @EventListener
    public void init(ApplicationStartedEvent event) throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        createSetting(AccessSettings.MODE_SETTING, "Acceptance Mode", AccessSettings.DEFAULT_MODE.getName());
        createSetting(AccessSettings.DEFAULT_ROLE_SETTING, "Default Role", DefaultRole.REGISTERED_USER.toString());
        createSetting(AccessSettings.DEFAULT_GROUPS_SETTING, "Default Groups", new ArrayList<>());
    }

    public boolean isAutoAccept() {
        boolean isAutoAccept = false;
        try {
            isAutoAccept = AccessSettings.AcceptanceMode.AUTO_ACCEPT.equals(currentMode());
        } catch (EntityNotFoundException e) {
            // do Nothing
        }
        return isAutoAccept;
    }

    private <T> void createSetting(String name, String description, T defaultValue) throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        DynamicTenantSetting dynamicTenantSetting;
        try {
            dynamicTenantSettingService.read(name);
        } catch (EntityNotFoundException e) {
            dynamicTenantSetting = new DynamicTenantSetting(name, description, defaultValue);
            dynamicTenantSettingService.create(dynamicTenantSetting);
        }
    }

    private AccessSettings.AcceptanceMode currentMode() throws EntityNotFoundException {
        return AccessSettings.AcceptanceMode.fromName(dynamicTenantSettingService.read(AccessSettings.MODE_SETTING).getValue());
    }

}
