/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao;

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

import fr.cnes.regards.microservices.core.dao.pojo.instance.Project;
import fr.cnes.regards.microservices.core.dao.pojo.projects.Company;
import fr.cnes.regards.microservices.core.dao.pojo.projects.User;
import fr.cnes.regards.microservices.core.dao.repository.instance.ProjectRepository;
import fr.cnes.regards.microservices.core.dao.repository.projects.CompanyRepository;
import fr.cnes.regards.microservices.core.dao.repository.projects.UserRepository;
import fr.cnes.regards.microservices.core.dao.util.CurrentTenantIdentifierResolverMock;
import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;

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

    /**
     * JPA Project repository
     */
    @Autowired
    private ProjectRepository projectRepository;

    /**
     * JPA User repository
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * JPA Company repository
     */
    @Autowired
    private CompanyRepository companyRepository;

    /**
     * Mock to overload tenant resolver. Allow to set tenant manually.
     */
    @Autowired
    private CurrentTenantIdentifierResolverMock tenantResolver;

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
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check JPA foreign keys management")
    @Test
    public void foreignKeyTests() {

        tenantResolver.setTenant(TENANT_TEST_1);

        userRepository.deleteAll();

        final Company comp = companyRepository.save(new Company("plop"));

        userRepository.save(new User("name", "lastname", comp));

        Assert.assertNotNull(userRepository.findAll().iterator().next().getCompany().getId().equals(comp.getId()));

    }

    /**
     *
     * Unit test to check JPA uses the good tenant through the tenant resolver
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check JPA uses the good tenant through the tenant resolver")
    @Test
    public void multitenancyAccessTest() {

        final List<Project> resultsP = new ArrayList<>();
        final List<User> results = new ArrayList<>();

        // Delete all previous data if any
        projectRepository.deleteAll();

        // Add a new Project
        final Project newProject = new Project();
        newProject.setName("Project 1");
        projectRepository.save(newProject);

        // Check results
        final Iterable<Project> listP = projectRepository.findAll();
        listP.forEach(project -> resultsP.add(project));
        Assert.assertTrue(String.format(
                                        "Error, there must be 1 elements in database associated to instance (%d)",
                                        resultsP.size()),
                          resultsP.size() == 1);

        // Set tenant to project test1
        tenantResolver.setTenant(TENANT_TEST_1);
        // Delete all previous data if any
        userRepository.deleteAll();
        // Set tenant to project 2
        tenantResolver.setTenant(TENANT_TEST_2);
        // Delete all previous data if any
        userRepository.deleteAll();

        // Set tenant to project test1
        tenantResolver.setTenant(TENANT_TEST_1);
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
        tenantResolver.setTenant(TENANT_TEST_2);

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
        tenantResolver.setTenant(TENANT_INVALID);
        try {
            // Check that an exception is thrown
            list = userRepository.findAll();
            Assert.fail("This repository is not valid for tenant");
        } catch (final CannotCreateTransactionException e) {
            // Nothing to do
        }

    }

}
