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
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.UUID;

import static fr.cnes.regards.modules.access.services.rest.user.mock.UserResourceClientMock.RESOURCES_ACCESS_STUB;

/**
 * Integration tests for UserResource REST Controller.
 */
@MultitenantTransactional
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=access"},
    locations = { "classpath:application-test.properties" })
public class UserResourceControllerIT extends AbstractRegardsTransactionalIT {

    private static final String DUMMY_USER_LOGIN = UUID.randomUUID().toString();

    @Test
    public void retrieveProjectUserResources() {
        String api = UserResourceController.TYPE_MAPPING;

        RequestBuilderCustomizer customizer =
            customizer()
                .expectStatusOk();

        performDefaultGet(api, customizer, "Failed to retrieve project user resources", DUMMY_USER_LOGIN);
    }

    @Test
    public void updateProjectUserResources() {
        String api = UserResourceController.TYPE_MAPPING;

        RequestBuilderCustomizer customizer =
            customizer()
                .expectStatusOk();

        performDefaultPut(api, Collections.singleton(RESOURCES_ACCESS_STUB), customizer, "Failed to update project user resource", DUMMY_USER_LOGIN);
    }

    @Test
    public void removeProjectUserResources() {
        String api = UserResourceController.TYPE_MAPPING;

        RequestBuilderCustomizer customizer =
            customizer()
                .expectStatusOk();

        performDefaultDelete(api, customizer, "Failed to remove project user resource", DUMMY_USER_LOGIN);
    }
}
