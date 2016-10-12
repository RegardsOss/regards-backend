/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.dao.stub.ProjectConnectionRepositoryStub;
import fr.cnes.regards.modules.project.dao.stub.ProjectRepositoryStub;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

public class ProjectServiceTest {

    private ProjectService projectService;

    @Before
    public void init() throws AlreadyExistingException {
        // use a stub repository, to be able to only test the service
        final IProjectRepository projectRepoStub = new ProjectRepositoryStub();
        final IProjectConnectionRepository projectConnectionRepoStub = new ProjectConnectionRepositoryStub();
        projectService = new ProjectService(projectRepoStub, projectConnectionRepoStub);

        final Project project1 = projectRepoStub.save(new Project(0L, "description", "icon", true, "project-test-1"));
        final Project project2 = projectRepoStub.save(new Project(1L, "description", "icon", true, "project-test-2"));

        projectConnectionRepoStub
                .save(new ProjectConnection(0L, project1, "microservice-test-1", "user", "pwd", "driver", "url"));
        projectConnectionRepoStub
                .save(new ProjectConnection(1L, project2, "microservice-test-2", "user", "pwd", "driver", "url"));
    }

    /**
     *
     * Test creation of a new database connection for a given project and a given microservice
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test creation of a new database connection for a given project and a given microservice.")
    @Test
    public void createProjectConnection() {

        final Project project = projectService.retrieveProject("project-test-1");
        final ProjectConnection connection = new ProjectConnection(600L, project, "microservice-test", "user", "pwd",
                "driver", "url");
        try {
            projectService.createProjectConnection(connection);
        } catch (final AlreadyExistingException | EntityNotFoundException e) {
            Assert.fail(e.getMessage());
        }

        try {
            projectService.createProjectConnection(connection);
            Assert.fail("Impossible to add two project connection for same project and microservice");
        } catch (final AlreadyExistingException e) {
            // Noting to do
        } catch (final EntityNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Test deletion of a database connection for a given project and a given microservice.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test deletion of a database connection for a given project and a given microservice.")
    @Test
    public void deleteProjectConnection() {
        ProjectConnection connection = projectService.retreiveProjectConnection("project-test-2",
                                                                                "microservice-test-2");
        try {
            projectService.deleteProjectConnection(connection.getId());
        } catch (final EntityNotFoundException e1) {
            Assert.fail(e1.getMessage());
        }

        connection = projectService.retreiveProjectConnection("project-test-2", "microservice-test-2");

        Assert.assertNull("Deletion error. Project connection always exists.", connection);

        try {
            projectService.deleteProjectConnection(56L);
            Assert.fail("Error the deletion should be in error. The entity doest not exists.");
        } catch (final EntityNotFoundException e) {
            // Nothing to do
        }

    }

    /**
     *
     * Test updating of a database connection for a given project and a given microservice.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test updating of a database connection for a given project and a given microservice.")
    @Test
    public void updateProjectConnection() {

        ProjectConnection connection = projectService.retreiveProjectConnection("project-test-1",
                                                                                "microservice-test-1");
        connection.setUserName("newUser");
        try {
            connection = projectService.updateProjectConnection(connection);
            Assert.assertTrue("Error updating project connection.", connection.getUserName().equals("newUser"));
        } catch (final EntityNotFoundException e1) {
            Assert.fail(e1.getMessage());
        }

        // Updating with an non existing project
        connection = new ProjectConnection(0L, new Project("description", "icon", true, "project-test-3"),
                "microservice-test", "user", "pwd", "driver", "url");
        try {
            connection = projectService.updateProjectConnection(connection);
            Assert.fail("Error the update should be in error. The entity doest not exists.");
        } catch (final EntityNotFoundException e) {
            // Nothing to do
        }

        // Updating a non existing projectConnection
        connection = new ProjectConnection(56L, new Project(0L, "description", "icon", true, "project-test-3"),
                "microservice-test", "user", "pwd", "driver", "url");
        try {
            connection = projectService.updateProjectConnection(connection);
            Assert.fail("Error the update should be in error. The entity doest not exists.");
        } catch (final EntityNotFoundException e) {
            // Nothing to do
        }

    }

}
