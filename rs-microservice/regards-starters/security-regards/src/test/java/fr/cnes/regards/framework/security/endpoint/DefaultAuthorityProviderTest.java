/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fr.cnes.regards.framework.security.domain.ResourceMapping;

/**
 *
 * Class DefaultAuthorityProviderTest
 *
 * Test class for IAuthoritiesProvider default implemetation
 *
 * @author sbinda
 * @since 1.0-SNAPSHT
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class DefaultAuthorityProviderTest {

    /**
     * Default auhtorities provider defined in configuration class
     */
    @Autowired
    private IAuthoritiesProvider provider;

    /**
     *
     * defaultAuthorityProviderTest
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void defaultAuthorityProviderTest() {

        final int three = 3;
        final List<ResourceMapping> results = provider.getResourcesAccessConfiguration();
        Assert.assertEquals(results.size(), three);

        Assert.assertTrue(provider.getRoleAuthorizedAddress("").isEmpty());
    }

}
