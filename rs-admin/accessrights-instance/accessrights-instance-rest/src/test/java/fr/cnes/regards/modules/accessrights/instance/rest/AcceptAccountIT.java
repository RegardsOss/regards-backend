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
package fr.cnes.regards.modules.accessrights.instance.rest;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.authentication.client.IExternalAuthenticationClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

/**
 * Specific integration test for 'accesses/acceptAccount' endpoint
 *
 * @author Xavier-Alexandre Brochard
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class AcceptAccountIT extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AcceptAccountIT.class);

    /**
     * Dummy email
     */
    private static final String EMAIL = "AcceptAccountIT@test.com";

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
    private IAccountRepository accountRepository;

    private Account account;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @MockBean
    private IExternalAuthenticationClient externalAuthenticationClient;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        account = accountRepository.save(new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD));
    }

    @After
    public void tearDown() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        accountRepository.delete(account);
    }

    /**
     * Check that the system allows an admin to manually accept an account.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_510")
    @Purpose("Check that the system allows an admin to manually accept an account.")
    public void acceptAccount() {
        String endpoint = AccountsController.TYPE_MAPPING + AccountsController.ACCEPT_ACCOUNT_PATH;

        performDefaultPut(endpoint, null, customizer().expectStatusOk(), "Unable to accept the account", EMAIL);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
