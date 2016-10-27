/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class DefaultPluginManagementTest
 *
 * Test class for default IPLuginResourceManagement implemenation
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class DefaultPluginManagementTest {

    /**
     * Default plugin resource manager defined in configuration class.
     */
    @Autowired
    private IPluginResourceManager manager;

    /**
     *
     * defaultPluginResourceManagerTest
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify access to plugin resources per microservice")
    @Test
    public void defaultPluginResourceManagerTest() {

        Assert.assertTrue(manager.manageMethodResource(new ResourceMapping("resource/path", RequestMethod.GET))
                .isEmpty());
    }

}
