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
import fr.cnes.regards.modules.core.hateoas.HateoasKeyWords;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.dao.stub.ProjectConnectionRepositoryStub;
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

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_DESCRIPTION = "description";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_ICON = "icon";

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
        projectRepo.save(new Project(0L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, "project-0"));
        projectRepo.save(new Project(1L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, "project-1"));
        projectRepo.save(new Project(2L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, "project-2"));

    }

    /**
     *
     * TODO
     *
     * @since TODO
     */
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to project resources and Hateoas links")
    @Test
    public void testRetrieveAllProjects() {
        try {
            final HttpEntity<List<Resource<Project>>> results = projectsController.retrieveProjectList();
            final List<Resource<Project>> resources = results.getBody();
            Assert.assertTrue("Error there must be project results", !resources.isEmpty());
            for (final Resource<Project> resource : resources) {
                final Project project = resource.getContent();
                Assert.assertNotNull("Result project shouldn't be null", project);
                // Check for Hateoas links
                Link link = resource.getLink(HateoasKeyWords.DELETE.getValue());
                Assert.assertNotNull("Delete hateoas link shouldn't be null", link);
                link = resource.getLink(HateoasKeyWords.SELF.getValue());
                Assert.assertNotNull("Self hateoas link shouldn't be null", link);
            }
            // CHECKSTYLE:OFF
        } catch (final Exception e) {
            // CHECKSTYLE:ON
            Assert.fail(e.getMessage());
        }
    }

}
