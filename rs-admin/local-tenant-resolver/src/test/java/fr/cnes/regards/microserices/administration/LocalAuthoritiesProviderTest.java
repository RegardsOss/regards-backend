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

import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

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
     * Authorities provider to test
     */
    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    /**
     *
     * Check cors requests access by role with date limitation
     *
     * @throws SecurityException
     *             when no role with passed name could be found
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_030")
    @Requirement("REGARDS_DSL_SYS_ARC_040")
    @Purpose("Check cors requests access by role with date limitation")
    @Test
    public void checkCorsRequestsAccessByRole() throws ModuleEntityNotFoundException, SecurityException {
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
        resourcesAccessRepository.deleteAll();
        resourcesAccessRepository.save(new ResourcesAccess("desc0", "ms0", "res0", HttpVerb.GET));
        resourcesAccessRepository.save(new ResourcesAccess("desc1", "ms1", "res1", HttpVerb.POST));
        Assert.assertEquals(provider.getResourcesAccessConfiguration().size(), 2);
    }

}
