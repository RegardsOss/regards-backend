package fr.cnes.regards.modules.plugins.domain;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author cmertz
 *
 */
public class PluginConfigurationTest extends PluginDomainUtility {

    @Test
    public void testDomain() {
        final PluginConfiguration pluginConfiguration = getPluginConfigurationWithDynamicParameter();
        final PluginMetaData pluginMetaData = getPluginMetaData();
        Assert.assertNotNull(pluginMetaData);
        Assert.assertNotNull(pluginConfiguration);
    }

}
