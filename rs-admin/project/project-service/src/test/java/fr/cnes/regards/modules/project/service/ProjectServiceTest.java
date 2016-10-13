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

/**
 *
 * Class ProjectServiceTest
 *
 * Project business service tests
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class ProjectServiceTest {

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_DESCRIPTION = "description";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_ICON = "icon";

    /**
     * Common string value for project creation.
     */
    private static final String PROJECT_TEST_1 = "project-test-1";

    /**
     * Common string value for project creation.
     */
    private static final String PROJECT_TEST_2 = "project-test-2";

    /**
     * Common string value for project creation.
     */
    private static final String PROJECT_TEST_3 = "project-test-3";

    /**
     * Common string value for project creation.
     */
    private static final String MS_TEST_1 = "ms-test-1";

    /**
     * Common string value for project creation.
     */
    private static final String MS_TEST_2 = "ms-test-2";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_USER_NAME = "username";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_USER_PWD = "password";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_DRIVER = "driver";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_URL = "url";

    /**
     * Project service to test.
     */
    private ProjectService projectService;

    /**
     *
     * Initializa DAO Stub and inline entities for tests
     *
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void init() {
        // use a stub repository, to be able to only test the service
        final IProjectRepository projectRepoStub = new ProjectRepositoryStub();
        final IProjectConnectionRepository projectConnectionRepoStub = new ProjectConnectionRepositoryStub();
        projectService = new ProjectService(projectRepoStub, projectConnectionRepoStub);

        final Project project1 = projectRepoStub
                .save(new Project(0L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_1));
        final Project project2 = projectRepoStub
                .save(new Project(1L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_2));

        projectConnectionRepoStub.save(new ProjectConnection(0L, project1, MS_TEST_1, COMMON_PROJECT_USER_NAME,
                COMMON_PROJECT_USER_PWD, COMMON_PROJECT_DRIVER, COMMON_PROJECT_URL));
        projectConnectionRepoStub.save(new ProjectConnection(1L, project2, MS_TEST_2, COMMON_PROJECT_USER_NAME,
                COMMON_PROJECT_USER_PWD, COMMON_PROJECT_DRIVER, COMMON_PROJECT_URL));
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

        final Project project = projectService.retrieveProject(PROJECT_TEST_1);
        final ProjectConnection connection = new ProjectConnection(600L, project, "microservice-test",
                COMMON_PROJECT_USER_NAME, COMMON_PROJECT_USER_PWD, COMMON_PROJECT_DRIVER, COMMON_PROJECT_URL);
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
        ProjectConnection connection = projectService.retreiveProjectConnection(PROJECT_TEST_2, MS_TEST_2);
        try {
            projectService.deleteProjectConnection(connection.getId());
        } catch (final EntityNotFoundException e1) {
            Assert.fail(e1.getMessage());
        }

        connection = projectService.retreiveProjectConnection(PROJECT_TEST_2, MS_TEST_1);

        Assert.assertNull("Deletion error. Project connection always exists.", connection);

        try {
            final long id = 5556L;
            projectService.deleteProjectConnection(id);
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

        final String updateUserName = "newUser";
        final String errorUpdate = "Error the update should be in error. The entity doest not exists.";
        ProjectConnection connection = projectService.retreiveProjectConnection(PROJECT_TEST_1, MS_TEST_1);
        connection.setUserName(updateUserName);
        try {
            connection = projectService.updateProjectConnection(connection);
            Assert.assertTrue("Error updating project connection.", connection.getUserName().equals(updateUserName));
        } catch (final EntityNotFoundException e1) {
            Assert.fail(e1.getMessage());
        }

        // Updating with an non existing project
        connection = new ProjectConnection(0L,
                new Project(COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_3), MS_TEST_1,
                COMMON_PROJECT_USER_NAME, COMMON_PROJECT_USER_PWD, COMMON_PROJECT_DRIVER, COMMON_PROJECT_URL);
        try {
            connection = projectService.updateProjectConnection(connection);
            Assert.fail(errorUpdate);
        } catch (final EntityNotFoundException e) {
            // Nothing to do
        }

        // Updating a non existing projectConnection
        final long id = 56L;
        connection = new ProjectConnection(id,
                new Project(0L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_3), MS_TEST_1,
                COMMON_PROJECT_USER_NAME, COMMON_PROJECT_USER_PWD, COMMON_PROJECT_DRIVER, COMMON_PROJECT_URL);
        try {
            connection = projectService.updateProjectConnection(connection);
            Assert.fail(errorUpdate);
        } catch (final EntityNotFoundException e) {
            // Nothing to do
        }

    }

}
