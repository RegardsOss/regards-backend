/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.rest;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.hateoas.HateoasKeyWords;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.dao.stub.ProjectConnectionRepositoryStub;
import fr.cnes.regards.modules.project.dao.stub.ProjectRepositoryStub;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.ProjectService;
import fr.cnes.regards.modules.project.signature.IProjectsSignature;

/**
 *
 * Class ProjectsControllerTest
 *
 * Tests for REST package
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class ProjectsControllerTest {

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
    private static final String PROJECT_TEST_0 = "project-0";

    /**
     * Common string value for project creation.
     */
    private static final String MICROSERVICE_TEST = "microservice";

    /**
     * Common string value for project creation.
     */
    private static final String MICROSERVICE_TEST_2 = "microservice-2";

    /**
     * Error message
     */
    private static final String HATEOAS_MISSING = "%s hateoas link shouldn't be null";

    /**
     * Project controller to test
     */
    private IProjectsSignature projectsController;

    /**
     * Stub for JPA Repository
     */
    private final IProjectRepository projectRepo = new ProjectRepositoryStub();

    /**
     * Stub for JPA Respository
     */
    private final IProjectConnectionRepository projectConnRepo = new ProjectConnectionRepositoryStub();

    private Project existingProject;

    /**
     *
     * Initialization for tests.
     *
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void initMethod() {

        final MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        projectsController = new ProjectsController(new ProjectService(projectRepo, projectConnRepo));

        // Initialization of in-lines entities
        existingProject = new Project(0L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_0);
        projectRepo.save(existingProject);
        projectRepo.save(new Project(1L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, "project-1"));
        projectRepo.save(new Project(2L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, "project-2"));

        projectConnRepo.save(new ProjectConnection(0L, existingProject, MICROSERVICE_TEST, "username", "password",
                "dirver", "url"));
        projectConnRepo.save(new ProjectConnection(1L, existingProject, MICROSERVICE_TEST_2, "username2", "password2",
                "dirver2", "url2"));

    }

    /**
     *
     * Check REST Access to project resources and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to project resources and returned Hateoas links")
    @Test
    public void retrieveAllProjectsTest() {
        try {
            final HttpEntity<List<Resource<Project>>> results = projectsController.retrieveProjectList();
            final List<Resource<Project>> resources = results.getBody();
            Assert.assertTrue("Error there must be project results", !resources.isEmpty());
            for (final Resource<Project> resource : resources) {
                final Project project = resource.getContent();
                Assert.assertNotNull("Result project shouldn't be null", project);
                // Check for Hateoas links
                Link link = resource.getLink(HateoasKeyWords.DELETE.getValue());
                Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.DELETE), link);
                link = resource.getLink(HateoasKeyWords.SELF.getValue());
                Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.SELF), link);
            }
            // CHECKSTYLE:OFF
        } catch (final Exception e) {
            // CHECKSTYLE:ON
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check REST Access to project resource and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to project resource and Hateoas returned links")
    @Test
    public void retrieveProjectTest() {
        try {
            final HttpEntity<Resource<Project>> result = projectsController.retrieveProject(PROJECT_TEST_0);
            final Resource<Project> resource = result.getBody();
            final Project project = resource.getContent();
            Assert.assertNotNull("The project result shouldn't be null", project);
            Link link = resource.getLink(HateoasKeyWords.DELETE.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.DELETE), link);
            link = resource.getLink(HateoasKeyWords.SELF.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.SELF), link);
            // Check for hateoas links
        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check REST Access for project creation and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access for project creation and Hateoas returned links")
    @Test
    public void createProjectTest() {

        final Project project = new Project(10L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true,
                "create-project");
        try {
            final HttpEntity<Resource<Project>> result = projectsController.createProject(project);
            final Resource<Project> resource = result.getBody();
            final Project createdProject = resource.getContent();
            Assert.assertNotNull("Error during project creation.", createdProject);
            Link link = resource.getLink(HateoasKeyWords.DELETE.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.DELETE), link);
            link = resource.getLink(HateoasKeyWords.SELF.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.SELF), link);
        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }

    }

    /**
     *
     * Check REST Access for project update and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access for project update and Hateoas returned links")
    @Test
    public void updateProjectTest() {
        try {
            final HttpEntity<Resource<Project>> result = projectsController.updateProject(existingProject.getName(),
                                                                                          existingProject);
            final Resource<Project> resource = result.getBody();
            final Project createdProject = resource.getContent();
            Assert.assertNotNull("Error during project update.", createdProject);
            Link link = resource.getLink(HateoasKeyWords.DELETE.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.DELETE), link);
            link = resource.getLink(HateoasKeyWords.SELF.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.SELF), link);
        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check REST Access to get a project connection and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to get a project connection and Hateoas returned links")
    @Test
    public void retrieveProjectConnection() {

        try {
            final HttpEntity<Resource<ProjectConnection>> result = projectsController
                    .retrieveProjectConnection(PROJECT_TEST_0, MICROSERVICE_TEST);
            final Resource<ProjectConnection> resource = result.getBody();
            final ProjectConnection connection = resource.getContent();
            Assert.assertNotNull("Error during project update.", connection);
            Link link = resource.getLink(HateoasKeyWords.DELETE.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.DELETE), link);
            link = resource.getLink(HateoasKeyWords.SELF.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.SELF), link);
            link = resource.getLink(HateoasKeyWords.CREATE.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.CREATE), link);
            link = resource.getLink(HateoasKeyWords.UPDATE.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.UPDATE), link);
        } catch (final EntityNotFoundException e) {
            Assert.fail(e.getMessage());
        }

    }

    /**
     *
     * Check REST Access to create a project connection and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to create a project connection and Hateoas returned links")
    @Test
    public void createProjectConnection() {
        try {
            final HttpEntity<Resource<ProjectConnection>> result = projectsController
                    .createProjectConnection(new ProjectConnection(2L, existingProject, "new description",
                            "newUserName", "newPassword", "newDriver", "newUrl"));
            final Resource<ProjectConnection> resource = result.getBody();
            final ProjectConnection connection = resource.getContent();
            Assert.assertNotNull("Error during project update.", connection);
            Link link = resource.getLink(HateoasKeyWords.DELETE.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.DELETE), link);
            link = resource.getLink(HateoasKeyWords.SELF.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.SELF), link);
            link = resource.getLink(HateoasKeyWords.CREATE.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.CREATE), link);
            link = resource.getLink(HateoasKeyWords.UPDATE.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.UPDATE), link);

        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check REST Access to update a project connection and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to update a project connection and Hateoas returned links")
    @Test
    public void updateProjectConnection() {
        try {
            final HttpEntity<Resource<ProjectConnection>> result = projectsController
                    .updateProjectConnection(new ProjectConnection(0L, existingProject, "update description",
                            "updateUserName", "updatePassword", "updateDriver", "updateUrl"));
            final Resource<ProjectConnection> resource = result.getBody();
            final ProjectConnection connection = resource.getContent();
            Assert.assertNotNull("Error during project update.", connection);
            Link link = resource.getLink(HateoasKeyWords.DELETE.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.DELETE), link);
            link = resource.getLink(HateoasKeyWords.SELF.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.SELF), link);
            link = resource.getLink(HateoasKeyWords.CREATE.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.CREATE), link);
            link = resource.getLink(HateoasKeyWords.UPDATE.getValue());
            Assert.assertNotNull(String.format(HATEOAS_MISSING, HateoasKeyWords.UPDATE), link);

        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check REST Access to update a project connection and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to update a project connection and Hateoas returned links")
    @Test
    public void deleteProjectConnection() {

        // Test for inexisting project connection deletion
        try {
            projectsController.deleteProjectConnection("project-invalid", MICROSERVICE_TEST_2);
            Assert.fail("Project connection doesn't exists. There should be an exception thrown here.");
        } catch (final EntityNotFoundException e1) {
            // Nothing to do
        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }

        // Delete existing entity
        try {
            projectsController.deleteProjectConnection(PROJECT_TEST_0, MICROSERVICE_TEST_2);

        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }
    }

}
