/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.controller;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class ResourcesControllerTest
 *
 * Tests for resources controller
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class ResourcesControllerTest {

    /**
     * SecurityResourcesController
     */
    private final SecurityResourcesController controller = new SecurityResourcesController(
            new MethodAuthorizationService());

    /**
     *
     * Check the /resources common endpoint that retrieves all resources of a microservice.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_ADM_ADM_240")
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Check the /resources common endpoint that retrieves all resources of a microservice.")
    @Test
    public void resourcesTest() {

        final ResponseEntity<List<ResourceMapping>> response = controller.getAllResources();
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
        // CHECKSTYLE:OFF
        Assert.assertTrue("There should be 2 resources", response.getBody().size() == 2);
        Assert.assertTrue(response.getBody().get(0).getFullPath().equals("/tests/endpoint"));
        Assert.assertTrue(response.getBody().get(0).getMethod().equals(RequestMethod.GET));
        Assert.assertTrue(response.getBody().get(1).getFullPath().equals("/tests/endpoint/post"));
        Assert.assertTrue(response.getBody().get(1).getMethod().equals(RequestMethod.POST));
        // CHECKSTYLE:ON
    }

}
