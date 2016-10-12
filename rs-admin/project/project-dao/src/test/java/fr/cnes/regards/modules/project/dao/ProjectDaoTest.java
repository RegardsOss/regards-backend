/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class ProjectDaoTest
 *
 * Test class for DAO of project module
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ProjectDaoTestConfiguration.class })
@DirtiesContext
public class ProjectDaoTest {

    /**
     * Project Repository
     */
    @Autowired
    private IProjectRepository projectRepository;

    /**
     * ProjectConnection Repository
     */
    @Autowired
    IProjectConnectionRepository projectConnectionRepository;

    /**
     *
     * Test to create and retrieve projects connections in instance database
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test to create and retrieve projects connections in instance database.")
    @Test
    public void createAndRetreiveProjectConnections() {

        final String microservice = "microservice-test";
        final String microservice2 = "microservice-test-2";

        // First clean all elements from databse
        projectConnectionRepository.deleteAll();
        projectRepository.deleteAll();

        // Create a new projects
        final Project project = projectRepository.save(new Project("description", "icon", true, "project-test"));
        projectRepository.save(new Project("description", "icon", true, "project-test-2"));

        // Check results
        final Iterable<Project> projects = projectRepository.findAll();
        final List<Project> results = new ArrayList<>();
        projects.forEach(p -> results.add(p));
        Assert.assertTrue(String.format("There must be 2 projects in database not %d", results.size()),
                          results.size() == 2);

        // Create new projects connections
        projectConnectionRepository
                .save(new ProjectConnection(project, microservice, "username", "password", "driver", "url"));
        projectConnectionRepository
                .save(new ProjectConnection(project, microservice2, "username", "password", "driver", "url"));

        // Check results
        final Iterable<ProjectConnection> connections = projectConnectionRepository.findAll();
        final List<ProjectConnection> cresults = new ArrayList<>();
        connections.forEach(c -> cresults.add(c));
        Assert.assertTrue(String.format("There must be 2 project connection in database not %d", cresults.size()),
                          cresults.size() == 2);
        final ProjectConnection conn = projectConnectionRepository
                .findOneByProjectNameAndMicroservice(project.getName(), microservice);
        Assert.assertNotNull(String.format(
                                           "Error retreiving project connection for project name %s and microservice %s",
                                           project.getName(), microservice),
                             conn);
        Assert.assertTrue("Error retreiving project connection for project name %s and microservice %s.",
                          conn.getMicroservice().equals(microservice));

    }

}
