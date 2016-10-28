package fr.cnes.regards.modules.plugins.domain;


import org.junit.Assert;
import org.junit.Test;



/**
 * 
 * @author cmertz
 *
 */
public class PluginConfigurationTest  {
    
    @Test
    public void testDomain() {
        final PluginConfiguration pluginConfiguration = PluginDomainUtility.getPluginConfigurationWithDynamicParameter();
        final PluginMetaData pluginMetaData = PluginDomainUtility.getPluginMetaData();
        Assert.assertNotNull(pluginMetaData);
        Assert.assertNotNull(pluginConfiguration);
    }

}
