/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * Specific integration test for 'accesses/refuseAccount' endpoint
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
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

    /**
     * A project user.<br>
     * We ensure before each test to have only this exactly project user in db for convenience.
     */
    private ProjectUser projectUser;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private IAccountRepository accountRepository;

    private Role publicRole;

    private Account account;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Do some setup before each test
     */
    @Before
    public void setUp() {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        account = accountRepository.save(new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD));
        projectUser = projectUserRepository
                .save(new ProjectUser(EMAIL, publicRole, new ArrayList<>(), new ArrayList<>()));
    }

    @After
    public void tearDown() {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        projectUserRepository.delete(projectUser);
        accountRepository.delete(account);
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
