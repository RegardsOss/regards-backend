/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microserices.administration;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class LocalAuthoritiesProviderTest
 *
 * Test for administration local AuthoritiesProvider
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { TestConfiguration.class })
public class LocalAuthoritiesProviderTest {

    /**
     * Authorities provider to test
     */
    @Autowired
    private IAuthoritiesProvider provider;

    /**
     *
     * Check cors requests access by role with date limitation
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_030")
    @Requirement("REGARDS_DSL_SYS_ARC_040")
    @Purpose("Check cors requests access by role with date limitation")
    @Test
    public void checkCorsRequestsAccessByRole() {

        Assert.assertEquals(provider.getRoleAuthorizedAddress(TestConfiguration.CORS_ROLE_NAME_GRANTED).size(), 3);
        Assert.assertTrue(provider.hasCorsRequestsAccess(TestConfiguration.CORS_ROLE_NAME_GRANTED));
        Assert.assertFalse(provider.hasCorsRequestsAccess(TestConfiguration.CORS_ROLE_NAME_INVALID_1));
        Assert.assertFalse(provider.hasCorsRequestsAccess(TestConfiguration.CORS_ROLE_NAME_INVALID_2));

    }

    /**
     *
     * Verify access to all resources access per microservice with internal administration microservice authorities.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify access to all resources access per microservice with internal administration microservice authorities.")
    @Test
    public void checkResourcesAcesses() {

        Assert.assertEquals(provider.getResourcesAccessConfiguration().size(), 4);
    }

}
