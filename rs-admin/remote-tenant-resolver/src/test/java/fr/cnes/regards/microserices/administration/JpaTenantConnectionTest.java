/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microserices.administration;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.microserices.administration.stubs.ProjectClientStub;
import fr.cnes.regards.microservices.administration.MicroserviceAutoConfiguration;

/**
 *
 * Class JpaTenantConnectionTest
 *
 * Test with jpa multitenant starter database creation.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { JpaTenantConnectionConfiguration.class, MicroserviceAutoConfiguration.class })
public class JpaTenantConnectionTest {

    /**
     *
     * Check for multitenant resolver throught administration microservice client
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Check for multitenant resolver throught administration microservice client")
    @Test
    public void checkJpaTenants() {

        File resourcesDirectory = new File("target/" + ProjectClientStub.PROJECT_NAME);
        Assert.assertTrue(resourcesDirectory.exists());

        resourcesDirectory = new File("target/test1");
        Assert.assertTrue(resourcesDirectory.exists());

        resourcesDirectory = new File("target/test2");
        Assert.assertTrue(resourcesDirectory.exists());

    }

}
