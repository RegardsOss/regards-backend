/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.project.client.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class ProjectClientFallbacksTests
 *
 * Tests for Projects client
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { ProjectClientFallbacksTestsConfiguration.class })
public class ProjectClientFallbacksTests {

    /**
     * Client to test
     */
    @Autowired
    private IProjectsClient projectsClient;

    /**
     *
     * Check for errors management for the admin REST microservice client
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_010")
    @Purpose("Check for errors management for the admin REST microservice client")
    @Test
    public void clientTest() {
        try {
            final String errorMessage = "Fallback error";
            final Project project = new Project(0L, "desc", "icon", true, "name");
            final ProjectConnection connection = new ProjectConnection(0L, project, "ms-test", "user", "pwd", "driver",
                    "url");
            ResponseEntity<?> results = projectsClient.retrieveProjectList();
            HttpStatus status = results.getStatusCode();
            Assert.assertTrue(errorMessage, status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = projectsClient.retrieveProject("project");
            status = results.getStatusCode();
            Assert.assertTrue(errorMessage, status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = projectsClient.createProject(project);
            status = results.getStatusCode();
            Assert.assertTrue(errorMessage, status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = projectsClient.updateProject(project.getName(), project);
            status = results.getStatusCode();
            Assert.assertTrue(errorMessage, status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = projectsClient.deleteProject(project.getName());
            status = results.getStatusCode();
            Assert.assertTrue(errorMessage, status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = projectsClient.retrieveProjectConnection(project.getName(), connection.getMicroservice());
            status = results.getStatusCode();
            Assert.assertTrue(errorMessage, status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = projectsClient.createProjectConnection(connection);
            status = results.getStatusCode();
            Assert.assertTrue(errorMessage, status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = projectsClient.updateProjectConnection(connection);
            status = results.getStatusCode();
            Assert.assertTrue(errorMessage, status.equals(HttpStatus.SERVICE_UNAVAILABLE));

            results = projectsClient.deleteProjectConnection(project.getName(), connection.getMicroservice());
            status = results.getStatusCode();
            Assert.assertTrue(errorMessage, status.equals(HttpStatus.SERVICE_UNAVAILABLE));

        } catch (final EntityException e) {
            Assert.fail(e.getMessage());
        }

    }

}
