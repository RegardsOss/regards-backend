/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.rest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.hateoas.DefaultResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.dao.stub.ProjectConnectionRepositoryStub;
import fr.cnes.regards.modules.project.dao.stub.ProjectRepositoryStub;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import fr.cnes.regards.modules.project.service.ProjectConnectionService;

/**
 *
 * Class ProjectsControllerTest
 *
 * Tests for REST package
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
public class ProjectConnectionControllerTest {

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
     * ProjectConnection controller to test
     */
    private ProjectConnectionController projectConnectionController;

    /**
     * Stub for JPA Repository
     */
    private final IProjectRepository projectRepo = new ProjectRepositoryStub();

    /**
     * Stub for JPA Respository
     */
    private final IProjectConnectionRepository projectConnRepo = new ProjectConnectionRepositoryStub();

    /**
     * Project instantiate in the init method for all tests.
     */
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

        final MethodAuthorizationService methodMocked = Mockito.mock(MethodAuthorizationService.class);
        Mockito.when(methodMocked.hasAccess(Mockito.any(), Mockito.any())).thenReturn(true);

        final IPublisher mockPublisher = Mockito.mock(IPublisher.class);

        projectConnectionController = new ProjectConnectionController(
                new ProjectConnectionService(projectRepo, projectConnRepo, mockPublisher),
                new DefaultResourceService(methodMocked));

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
     * Check REST Access to get a project connection and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to get a project connection and Hateoas returned links")
    @Test
    public void retrieveProjectConnection() {

        final HttpEntity<Resource<ProjectConnection>> result = projectConnectionController
                .retrieveProjectConnection(PROJECT_TEST_0, MICROSERVICE_TEST);
        final Resource<ProjectConnection> resource = result.getBody();
        final ProjectConnection connection = resource.getContent();
        Assert.assertNotNull("Error retrieving project connection.", connection);
        Link link = resource.getLink(LinkRels.DELETE);
        Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.DELETE), link);
        link = resource.getLink(LinkRels.SELF);
        Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.SELF), link);
        link = resource.getLink(LinkRels.CREATE);
        Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.CREATE), link);
        link = resource.getLink(LinkRels.UPDATE);
        Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.UPDATE), link);

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
        final HttpEntity<Resource<ProjectConnection>> result = projectConnectionController
                .createProjectConnection(new ProjectConnection(2L, existingProject, "new description", "newUserName",
                        "newPassword", "newDriver", "newUrl"));
        final Resource<ProjectConnection> resource = result.getBody();
        final ProjectConnection connection = resource.getContent();
        Assert.assertNotNull("Error during project connection creation.", connection);
        Link link = resource.getLink(LinkRels.DELETE);
        Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.DELETE), link);
        link = resource.getLink(LinkRels.SELF);
        Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.SELF), link);
        link = resource.getLink(LinkRels.CREATE);
        Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.CREATE), link);
        link = resource.getLink(LinkRels.UPDATE);
        Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.UPDATE), link);
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
        final HttpEntity<Resource<ProjectConnection>> result = projectConnectionController
                .updateProjectConnection(new ProjectConnection(0L, existingProject, "update description",
                        "updateUserName", "updatePassword", "updateDriver", "updateUrl"));
        final Resource<ProjectConnection> resource = result.getBody();
        final ProjectConnection connection = resource.getContent();
        Assert.assertNotNull("Error during project connection update.", connection);
        Link link = resource.getLink(LinkRels.DELETE);
        Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.DELETE), link);
        link = resource.getLink(LinkRels.SELF);
        Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.SELF), link);
        link = resource.getLink(LinkRels.CREATE);
        Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.CREATE), link);
        link = resource.getLink(LinkRels.UPDATE);
        Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.UPDATE), link);
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

        final ResponseEntity<Void> response = projectConnectionController.deleteProjectConnection("project-invalid",
                                                                                                  MICROSERVICE_TEST_2);

        Assert.assertTrue("Project connection doesn't exists. There should be an error",
                          response.getStatusCode().equals(HttpStatus.NOT_FOUND));

        projectConnectionController.deleteProjectConnection(PROJECT_TEST_0, MICROSERVICE_TEST_2);
    }

}
