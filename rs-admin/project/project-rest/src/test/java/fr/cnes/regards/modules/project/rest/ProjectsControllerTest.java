/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.rest;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import fr.cnes.regards.framework.hateoas.DefaultResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.dao.stub.ProjectRepositoryStub;
import fr.cnes.regards.modules.project.domain.Project;
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

    private static final Logger LOG = LoggerFactory.getLogger(ProjectsControllerTest.class);

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

        projectsController = new ProjectsController(new ProjectService(projectRepo),
                new DefaultResourceService(methodMocked));

        // Initialization of in-lines entities
        existingProject = new Project(0L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_0);
        projectRepo.save(existingProject);
        projectRepo.save(new Project(1L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, "project-1"));
        projectRepo.save(new Project(2L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, "project-2"));

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
            final ResponseEntity<List<Resource<Project>>> results = projectsController.retrieveProjectList();
            final List<Resource<Project>> resources = results.getBody();
            Assert.assertTrue("Error there must be project results", !resources.isEmpty());
            for (final Resource<Project> resource : resources) {
                final Project project = resource.getContent();
                Assert.assertNotNull("Result project shouldn't be null", project);
                // Check for Hateoas links
                Link link = resource.getLink(LinkRels.DELETE);
                Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.DELETE), link);
                link = resource.getLink(LinkRels.SELF);
                Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.SELF), link);
            }
            // CHECKSTYLE:OFF
        } catch (final Exception e) {
            // CHECKSTYLE:ON
            LOG.error(e.getMessage(), e);
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
            final ResponseEntity<Resource<Project>> result = projectsController.retrieveProject(PROJECT_TEST_0);
            final Resource<Project> resource = result.getBody();
            final Project project = resource.getContent();
            Assert.assertNotNull("The project result shouldn't be null", project);
            Link link = resource.getLink(LinkRels.DELETE);
            Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.DELETE), link);
            link = resource.getLink(LinkRels.SELF);
            Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.SELF), link);
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
            final ResponseEntity<Resource<Project>> result = projectsController.createProject(project);
            final Resource<Project> resource = result.getBody();
            final Project createdProject = resource.getContent();
            Assert.assertNotNull("Error during project creation.", createdProject);
            Link link = resource.getLink(LinkRels.DELETE);
            Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.DELETE), link);
            link = resource.getLink(LinkRels.SELF);
            Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.SELF), link);
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
            final ResponseEntity<Resource<Project>> result = projectsController.updateProject(existingProject.getName(),
                                                                                              existingProject);
            final Resource<Project> resource = result.getBody();
            final Project createdProject = resource.getContent();
            Assert.assertNotNull("Error during project update.", createdProject);
            Link link = resource.getLink(LinkRels.DELETE);
            Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.DELETE), link);
            link = resource.getLink(LinkRels.SELF);
            Assert.assertNotNull(String.format(HATEOAS_MISSING, LinkRels.SELF), link);
        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }
    }

}
