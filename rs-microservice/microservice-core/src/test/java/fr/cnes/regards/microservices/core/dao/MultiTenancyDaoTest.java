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

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { MultiTenancyDaoTestConfiguration.class })
@DirtiesContext
public class MultiTenancyDaoTest {

    static final Logger LOG = LoggerFactory.getLogger(MultiTenancyDaoTest.class);

    @Autowired
    private ProjectRepository projectRepository_;

    @Autowired
    private UserRepository userRepository_;

    @Autowired
    private CompanyRepository companyRepository_;

    @Autowired
    private CurrentTenantIdentifierResolverMock tenantResolver_;

    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check that the spring JPA multitenancy context is loaded successfully")
    @Test
    public void contextLoads() {
        // Nothing to do. Only tests if the spring context is ok.
    }

    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check JPA foreign keys management")
    @Test
    public void foreignKeyTests() {

        tenantResolver_.setTenant("test1");

        userRepository_.deleteAll();

        Company comp = companyRepository_.save(new Company("plop"));

        userRepository_.save(new User("Test", "Test", comp));

        Assert.assertNotNull(userRepository_.findAll().iterator().next().getCompany().getId().equals(comp.getId()));

    }

    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check JPA uses the good tenant through the tenant resolver")
    @Test
    public void multitenancyAccessTest() {

        List<Project> resultsP = new ArrayList<>();
        List<User> results = new ArrayList<>();

        // Delete all previous data if any
        projectRepository_.deleteAll();

        // Add a new Project
        Project newProject = new Project();
        newProject.setFirstName("Project 1");
        projectRepository_.save(newProject);

        // Check results
        Iterable<Project> listP = projectRepository_.findAll();
        listP.forEach(project -> resultsP.add(project));
        Assert.assertTrue("Error, there must be 1 elements in the database associated to the instance db("
                + resultsP.size() + ")", resultsP.size() == 1);

        // Set tenant to project test1
        tenantResolver_.setTenant("test1");
        // Delete all previous data if any
        userRepository_.deleteAll();
        // Set tenant to project 2
        tenantResolver_.setTenant("test2");
        // Delete all previous data if any
        userRepository_.deleteAll();

        // Set tenant to project test1
        tenantResolver_.setTenant("test1");
        // Add new users
        User newUser = new User("Jean", "Pont");
        newUser = userRepository_.save(newUser);
        LOG.info("id=" + newUser.getId());
        User newUser2 = new User("Alain", "Deloin");
        newUser2 = userRepository_.save(newUser2);
        LOG.info("id=" + newUser2.getId());

        // Check results
        Iterable<User> list = userRepository_.findAll();
        list.forEach(user -> results.add(user));
        Assert.assertTrue("Error, there must be 2 elements in the database associated to the tenant test1 not "
                + results.size(), results.size() == 2);

        // Set tenant to project 2
        tenantResolver_.setTenant("test2");

        // Check that there is no users added on this project
        list = userRepository_.findAll();
        results.clear();
        list.forEach(user -> results.add(user));
        Assert.assertTrue("Error, there must be no element in the database associated to the tenant test2 ("
                + results.size() + ")", results.size() == 0);

        newUser = userRepository_.save(newUser);
        LOG.info("id=" + newUser.getId());

        // Check results
        list = userRepository_.findAll();
        results.clear();
        list.forEach(user -> results.add(user));
        Assert.assertTrue("Error, there must be 1 elements in the database associated to the tenant test2 + not "
                + results.size(), results.size() == 1);

        // Set tenant to an non existing project
        tenantResolver_.setTenant("invalid");
        try {
            // Check that an exception is thrown
            list = userRepository_.findAll();
            Assert.fail("This repository is not valid for tenant");
        }
        catch (CannotCreateTransactionException e) {
            // Nothing to do
        }

    }

}
