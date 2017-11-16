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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.service.registration.IRegistrationService;

/**
 * Specific integration test for 'accesses/refuseAccount' endpoint
 *
 * @author Xavier-Alexandre Brochard
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class RefuseAccountIT extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RefuseAccountIT.class);

    /**
     * Dummy email
     */
    private static final String EMAIL = "RefuseAccountIT@test.com";

    /**
     * Dummy first name
     */
    private static final String FIRST_NAME = "Firstname";

    /**
     * Dummy last name
     */
    private static final String LAST_NAME = "Lastname";

    /**
     * Dummy password
     */
    private static final String PASSWORD = "password";

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IRegistrationService registrationService;

    /**
     * Do some setup before each test
     *
     * @throws EntityException
     */
    @Before
    public void setUp() throws EntityException {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        registrationService.requestAccess(new AccessRequestDto(EMAIL, FIRST_NAME, LAST_NAME, "REGISTERED_USER",
                new ArrayList<>(), PASSWORD, "originUrl", "requestLink"));
    }

    /**
     * Check that the system allows an admin to manually refuse an account.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system allows an admin to manually refuse an account.")
    public void refuseAccount() {
        String endpoint = RegistrationController.REQUEST_MAPPING_ROOT
                + RegistrationController.REFUSE_ACCOUNT_RELATIVE_PATH;

        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(endpoint, null, expectations, "Unable to refuse the account", EMAIL);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
