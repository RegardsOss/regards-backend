/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.CannotCreateTransactionException;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.pojo.Company;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.pojo.User;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.repository.ICompanyRepository;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.repository.IUserRepository;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.security.utils.jwt.exception.MissingClaimException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class MultiTenancyDaoTest
 *
 * Unit tests for multitenancy DAO
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { MultiTenancyDaoTestConfiguration.class })
@DirtiesContext
public class MultiTenancyDaoTest {

    /**
     * class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MultiTenancyDaoTest.class);

    /**
     * Tenant name for test1
     */
    private static final String TENANT_TEST_1 = "test1";

    /**
     * Tenant name for test2
     */
    private static final String TENANT_TEST_2 = "test2";

    /**
     * Tenant name for invalid tenant (does not exists)
     */
    private static final String TENANT_INVALID = "invalid";

    private static final String TEST_ROLE = "USER";

    private static final String INVALID_JWT = "Invalid JWT";

    /**
     * JPA User repository
     */
    @Autowired
    private IUserRepository userRepository;

    /**
     * JPA Company repository
     */
    @Autowired
    private ICompanyRepository companyRepository;

    /**
     * Security JWT management service
     */
    @Autowired
    private JWTService jwtService;

    /**
     *
     * Unit test to check that the spring JPA multitenancy context is loaded successfully
     *
     * @since 1.0-SNAPSHOTS
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check that the spring JPA multitenancy context is loaded successfully")
    @Test
    public void contextLoads() {
        // Nothing to do. Only tests if the spring context is ok.
    }

    /**
     *
     * Unit test to check JPA foreign keys management
     *
     * @throws MissingClaimException
     * @throws InvalidJwtException
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check JPA foreign keys management")
    @Test
    public void foreignKeyTests() {

        try {
            jwtService.injectToken(TENANT_TEST_1, TEST_ROLE);

            userRepository.deleteAll();

            final Company comp = companyRepository.save(new Company("plop"));

            userRepository.save(new User("name", "lastname", comp));

            Assert.assertNotNull(userRepository.findAll().iterator().next().getCompany().getId().equals(comp.getId()));
        } catch (final JwtException e) {
            LOG.error(INVALID_JWT);
            Assert.fail(INVALID_JWT);
        }

    }

    /**
     *
     * Unit test to check JPA uses the good tenant through the tenant resolver
     *
     * @throws MissingClaimException
     * @throws InvalidJwtException
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check that JPA uses the good tenant through the tenant resolver")
    @Test
    public void multitenancyAccessTest() {
        try {
            final List<User> results = new ArrayList<>();

            // Set tenant to project test1
            jwtService.injectToken(TENANT_TEST_1, TEST_ROLE);
            // Delete all previous data if any
            userRepository.deleteAll();
            // Set tenant to project 2
            jwtService.injectToken(TENANT_TEST_2, TEST_ROLE);
            // Delete all previous data if any
            userRepository.deleteAll();

            // Set tenant to project test1
            jwtService.injectToken(TENANT_TEST_1, TEST_ROLE);
            // Add new users
            User newUser = new User("Jean", "Pont");
            newUser = userRepository.save(newUser);
            User newUser2 = new User("Alain", "Deloin");
            newUser2 = userRepository.save(newUser2);

            // Check results
            Iterable<User> list = userRepository.findAll();
            list.forEach(user -> results.add(user));
            Assert.assertTrue("Error, there must be 2 elements in the database associated to the tenant test1 not "
                    + results.size(), results.size() == 2);

            // Set tenant to project 2
            jwtService.injectToken(TENANT_TEST_2, TEST_ROLE);

            // Check that there is no users added on this project
            list = userRepository.findAll();
            results.clear();
            list.forEach(user -> results.add(user));
            Assert.assertTrue("Error, there must be no element in the database associated to the tenant test2 ("
                    + results.size() + ")", results.size() == 0);

            newUser = userRepository.save(newUser);
            LOG.info("id=" + newUser.getId());

            // Check results
            list = userRepository.findAll();
            results.clear();
            list.forEach(user -> results.add(user));
            Assert.assertTrue("Error, there must be 1 elements in the database associated to the tenant test2 + not "
                    + results.size(), results.size() == 1);

            // Set tenant to an non existing project
            jwtService.injectToken(TENANT_INVALID, TEST_ROLE);
            try {
                // Check that an exception is thrown
                list = userRepository.findAll();
                Assert.fail("This repository is not valid for tenant");
            } catch (final CannotCreateTransactionException e) {
                // Nothing to do
            }
        } catch (final JwtException e) {
            LOG.error(INVALID_JWT);
            Assert.fail(INVALID_JWT);
        }
    }
}
