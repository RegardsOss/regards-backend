/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.instance.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.jpa.instance.autoconfigure.pojo.TestProject;
import fr.cnes.regards.framework.jpa.instance.autoconfigure.repository.IProjectTestRepository;
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
@ContextConfiguration(classes = { InstanceDaoTestConfiguration.class })
@DirtiesContext
public class InstanceDaoTest {

    /**
     * JPA Project repository
     */
    @Autowired
    private IProjectTestRepository projectRepository;

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
     * Unit test to check JPA uses the good tenant through the tenant resolver
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check JPA uses the good tenant through the tenant resolver")
    @Test
    public void multitenancyAccessTest() {

        final List<TestProject> resultsP = new ArrayList<>();

        // Delete all previous data if any
        projectRepository.deleteAll();

        // Add a new Project
        final TestProject newProject = new TestProject();
        newProject.setName("Project 1");
        projectRepository.save(newProject);

        // Check results
        final Iterable<TestProject> listP = projectRepository.findAll();
        listP.forEach(project -> resultsP.add(project));
        Assert.assertTrue(String.format("Error, there must be 1 elements in database associated to instance (%d)",
                                        resultsP.size()),
                          resultsP.size() == 1);

    }

}
