/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microserices.administration;

import java.io.File;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.microserices.administration.stubs.ProjectClientStub;
import fr.cnes.regards.microservices.administration.RemoteTenantAutoConfiguration;

/**
 *
 * Class JpaTenantConnectionTest
 *
 * Test with jpa multitenant starter database creation.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Ignore("Cannot reach an admin microservice instance in unit test.")
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { JpaTenantConnectionConfiguration.class, RemoteTenantAutoConfiguration.class })
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
