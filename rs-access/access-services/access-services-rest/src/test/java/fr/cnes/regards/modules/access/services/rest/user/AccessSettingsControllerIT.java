/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.rest.user;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSettingDto;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for AccessSettings REST Controller.
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=access" },
                    locations = { "classpath:application-test.properties" })
public class AccessSettingsControllerIT extends AbstractRegardsTransactionalIT {

    @Test
    public void retrieveAccessSettings() {
        String api = AccessSettingsController.REQUEST_MAPPING_ROOT;
        RequestBuilderCustomizer customizer = customizer().expectStatusOk().expectIsArray("$").expectToHaveSize("$", 5);
        performDefaultGet(api, customizer, "Failed to retrieve access settings");
    }

    @Test
    public void updateAccessSettings() {
        String api = AccessSettingsController.REQUEST_MAPPING_ROOT + AccessSettingsController.NAME_PATH;
        DynamicTenantSetting defaultAccessSetting = AccessSettings.MODE_SETTING;
        DynamicTenantSettingDto<String> dto = new DynamicTenantSettingDto<>(defaultAccessSetting.getName(),
                                                                            defaultAccessSetting.getDescription(),
                                                                            defaultAccessSetting.getDefaultValue(),
                                                                            defaultAccessSetting.getValue());
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        performDefaultPut(api, dto, customizer, "Failed to update access settings", dto.getName());
    }

}
