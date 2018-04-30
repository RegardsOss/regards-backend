/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.rest;

import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;

/**
 * Test {@link LicenseController}
 *
 * @author Marc Sordi
 *
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=license" })
public class LicenseControllerIT extends AbstractRegardsTransactionalIT {

    @Test
    @Purpose("Check license agreement can be reset by an ADMIN")
    public void resetLicense() {

        String INSTANCE_TENANT = "instance";
        String customToken = jwtService.generateToken(INSTANCE_TENANT, DEFAULT_USER_EMAIL,
                                                      DefaultRole.ADMIN.toString());
        authService.setAuthorities(INSTANCE_TENANT, LicenseController.PATH_LICENSE + LicenseController.PATH_RESET,
                                   "osef", RequestMethod.PUT, DefaultRole.ADMIN.toString());

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isNoContent());
        performPut(LicenseController.PATH_LICENSE + LicenseController.PATH_RESET, customToken, null,
                   requestBuilderCustomizer, "Error retrieving endpoints", DEFAULT_TENANT);
    }
}
