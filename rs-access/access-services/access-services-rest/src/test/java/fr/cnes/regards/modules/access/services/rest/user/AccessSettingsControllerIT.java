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
package fr.cnes.regards.modules.access.services.rest.user;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.access.services.domain.user.AccessSettingsDto;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import static fr.cnes.regards.modules.access.services.rest.user.mock.StorageSettingClientMock.DEFAULT_QUOTA_LIMITS_STUB_MAX_QUOTA;
import static fr.cnes.regards.modules.access.services.rest.user.mock.StorageSettingClientMock.DEFAULT_QUOTA_LIMITS_STUB_RATE_LIMIT;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;

/**
 * Integration tests for AccessSettings REST Controller.
 */
@MultitenantTransactional
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=access"},
    locations = { "classpath:application-test.properties" })
public class AccessSettingsControllerIT extends AbstractRegardsTransactionalIT {

    @Test
    public void retrieveAccessSettings() {
        String api = AccessSettingsController.REQUEST_MAPPING_ROOT;

        RequestBuilderCustomizer customizer =
            customizer()
                .expectStatusOk()
                .expectValue("$.content.mode", AccessSettings.MODE)
                .expectValue("$.content.maxQuota", DEFAULT_QUOTA_LIMITS_STUB_MAX_QUOTA)
                .expectValue("$.content.rateLimit", DEFAULT_QUOTA_LIMITS_STUB_RATE_LIMIT)
            ;

        performDefaultGet(api, customizer, "Failed to retrieve access settings");
    }

    @Test
    @Ignore("FIXME: to reactivate once API has been decided with front dev")
    public void updateAccessSettings() {
//        String api = AccessSettingsController.REQUEST_MAPPING_ROOT;
//
//        AccessSettingsDto dto = new AccessSettingsDto(
//            ACCESS_SETTINGS_STUB_ID,
//            ACCESS_SETTINGS_STUB_MODE,
//            ACCESS_SETTINGS_STUB_ROLE,
//            ACCESS_SETTINGS_STUB_GROUPS,
//            DEFAULT_QUOTA_LIMITS_STUB_MAX_QUOTA,
//            DEFAULT_QUOTA_LIMITS_STUB_RATE_LIMIT
//        );
//
//        RequestBuilderCustomizer customizer =
//            customizer()
//                .expectStatusOk()
//                .expectValue("$.content.id", ACCESS_SETTINGS_STUB_ID)
//                .expectValue("$.content.mode", ACCESS_SETTINGS_STUB_MODE)
//                .expectValue("$.content.role.name", ACCESS_SETTINGS_STUB_ROLE.getName())
//                .expectValue("$.content.groups[0]", ACCESS_SETTINGS_STUB_GROUPS.get(0))
//                .expectValue("$.content.maxQuota", DEFAULT_QUOTA_LIMITS_STUB_MAX_QUOTA)
//                .expectValue("$.content.rateLimit", DEFAULT_QUOTA_LIMITS_STUB_RATE_LIMIT)
//            ;
//
//        performDefaultPut(api, dto, customizer, "Failed to update access settings");
    }
}
