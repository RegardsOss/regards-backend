/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.client;

import org.junit.Assert;
import org.junit.Test;

import feign.Target.HardCodedTarget;

/**
 *
 * Class ResourcesClientTest
 *
 * Test creation of a feign client for security resources controller
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
public class ResourcesClientTest {

    /**
     *
     * Test creation of a feign client for security resources controller
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void resourcesClientBuildTest() {
        Assert.assertNotNull(IResourcesClient
                .build(new HardCodedTarget<IResourcesClient>(IResourcesClient.class, "url")));
    }

}
