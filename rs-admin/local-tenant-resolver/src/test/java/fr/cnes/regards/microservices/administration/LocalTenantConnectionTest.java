/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.microservices.administration.LocalTenantConnectionResolverAutoConfiguration;

/**
 *
 * Class JpaTenantConnectionTest
 *
 * Test with jpa multitenant starter database creation.
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@EnableAutoConfiguration
@ContextConfiguration(classes = { TenantTestConfiguration.class, LocalTenantConnectionResolverAutoConfiguration.class })
public class LocalTenantConnectionTest {

    /**
     *
     * Check for multitenant resolver throught administration microservice client
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Check for multitenant throught local resolver."
            + " The the tenant resolver should create a datasource not defined in application.properties")
    @Test
    public void checkJpaTenants() {

        final File resourcesDirectory = new File("target/" + AuthoritiesTestConfiguration.PROJECT_NAME);
        Assert.assertTrue(resourcesDirectory.exists());

    }

}
