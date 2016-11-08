/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.service;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.dao.stub.ProjectRepositoryStub;
import fr.cnes.regards.modules.project.domain.Project;

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
        projectService = new ProjectService(projectRepoStub);

        projectRepoStub.save(new Project(0L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_1));
        projectRepoStub.save(new Project(1L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_2));
    }

    /**
     *
     * Check that the system allows to create a project.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Purpose("Check that the system allows to create a project.")
    public void createProjectTest() {
        final long newProjectId = 2L;
        Project projectToCreate = new Project(newProjectId, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, false,
                PROJECT_TEST_1);
        try {
            projectService.createProject(projectToCreate);
            Assert.fail("Project already exists there must be an exception thrown here");
        } catch (final AlreadyExistingException e) {
            /// Nothing to do
        }
        projectToCreate = new Project(newProjectId, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, false,
                "new-project-test");
        try {
            projectService.createProject(projectToCreate);
        } catch (final AlreadyExistingException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the system allows to retrieve all projects for an instance.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Purpose("Check that the system allows to retrieve all projects for an instance.")
    public void retrieveAllProjectTest() {
        final List<Project> projects = projectService.retrieveProjectList();
        Assert.assertTrue("There must be projects.", !projects.isEmpty());
    }

    /**
     *
     * Check that the system allows to retrieve a project on an instance and handle fail cases.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Purpose("Check that the system allows to retrieve a project on an instance and handle fail cases.")
    public void getProjectTest() {
        try {
            projectService.retrieveProject("invalid_project_name");
        } catch (final EntityNotFoundException e) {
            // Nothing to do
        }

        try {
            projectService.retrieveProject(PROJECT_TEST_1);
        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the system allows to update a project on an instance and handle fail cases.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Purpose("Check that the system allows to update a project on an instance and handle fail cases.")
    public void updateProject() {

        final String invalidProjectName = "project-invalid-update";
        final Project invalidProject = new Project(COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, false,
                invalidProjectName);
        try {
            projectService.updateProject(invalidProjectName, invalidProject);
        } catch (final EntityNotFoundException e) {
            // Nothing to do
        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }

        Project existingProject = null;
        try {
            existingProject = projectService.retrieveProject(PROJECT_TEST_1);
            existingProject.setIcon("new-icon-update");
        } catch (final EntityException e1) {
            Assert.fail(e1.getMessage());
        }

        try {
            projectService.updateProject(invalidProjectName, existingProject);
        } catch (final EntityNotFoundException e) {
            // Nothing to do
        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }

        try {
            projectService.updateProject(PROJECT_TEST_1, existingProject);
        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull("The project to update exists. The returned project shouldn't be null", existingProject);

    }

}
