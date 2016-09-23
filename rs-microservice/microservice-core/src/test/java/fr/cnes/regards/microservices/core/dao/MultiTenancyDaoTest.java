/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.microservices.core.dao.instance.Project;
import fr.cnes.regards.microservices.core.dao.instance.ProjectRepository;
import fr.cnes.regards.microservices.core.dao.pojo.User;
import fr.cnes.regards.microservices.core.dao.repository.UserRepository;
import fr.cnes.regards.microservices.core.dao.util.CurrentTenantIdentifierResolverMock;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { MultiTenancyDaoTestConfiguration.class })
@DirtiesContext
public class MultiTenancyDaoTest {

    static final Logger LOG = LoggerFactory.getLogger(MultiTenancyDaoTest.class);

    @Autowired
    private ProjectRepository projectRepository_;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentTenantIdentifierResolverMock tenantResolver;

    @Autowired
    @Qualifier("instanceEntityManagerFactory")
    EntityManager em;

    @Test
    public void contextLoads() {
        // Nothing to do. Only tests if the spring context is ok.
    }

    @Test
    public void multitenancyAccessTest() {

        tenantResolver.setTenant("test1");

        projectRepository_.deleteAll();
        Project newProject = new Project();
        newProject.setFirstName("PLOP");
        Project pro = projectRepository_.save(newProject);

        List<Project> resultsP = new ArrayList<>();
        Iterable<Project> listP = projectRepository_.findAll();
        listP.forEach(project -> resultsP.add(project));
        Assert.assertTrue("Error, there must be 1 elements in the database associated to the tenant test1 ("
                + resultsP.size() + ")", resultsP.size() == 1);

        List<User> results = new ArrayList<>();

        tenantResolver.setTenant("test1");

        userRepository.deleteAll();
        User newUser = new User("Jean", "Pont");
        newUser = userRepository.save(newUser);
        LOG.info("id=" + newUser.getId());

        User newUser2 = new User("Alain", "Deloin");
        newUser2 = userRepository.save(newUser2);
        LOG.info("id=" + newUser2.getId());

        Iterable<User> list = userRepository.findAll();
        list.forEach(user -> results.add(user));

        Assert.assertTrue("Error, there must be 2 elements in the database associated to the tenant test1",
                          results.size() == 2);

        tenantResolver.setTenant("test2");
        list = userRepository.findAll();
        results.clear();
        list.forEach(user -> results.add(user));
        Assert.assertTrue("Error, there must be no element in the database associated to the tenant test1 ("
                + results.size() + ")", results.size() == 0);

    }

}
