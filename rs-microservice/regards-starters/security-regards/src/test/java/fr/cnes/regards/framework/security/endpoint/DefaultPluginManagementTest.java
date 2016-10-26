/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;

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
    @Test
    public void defaultPluginResourceManagerTest() {

        final String nameLabel = "name";
        final String descLabel = "description";

        // Initiate test ResourceAccess to serialize
        final Map<String, Object> attributs = new HashMap<>();
        attributs.put(nameLabel, nameLabel);
        attributs.put(descLabel, descLabel);
        final ResourceAccess resourceAccess = AnnotationUtils.synthesizeAnnotation(attributs, ResourceAccess.class,
                                                                                   null);

        attributs.clear();
        final RequestMapping requestMapping = AnnotationUtils.synthesizeAnnotation(attributs, RequestMapping.class,
                                                                                   null);
        attributs.put(nameLabel, nameLabel);
        attributs.put(descLabel, descLabel);
        Assert.assertTrue(manager.manageMethodResource("resource/path", resourceAccess, requestMapping).isEmpty());
    }

}
