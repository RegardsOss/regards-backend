package fr.cnes.regards.modules.plugins.domain;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author cmertz
 *
 */
public class PluginDomainTest extends PluginDomainUtility {

    /**
     * Test the {@link PluginConfiguration} creation.
     */
    @Test
    public void pluginConfigurationWithDynParameters() {
        final PluginConfiguration pluginConfiguration = getPluginConfigurationWithDynamicParameter();
        final PluginMetaData pluginMetaData = getPluginMetaData();
        Assert.assertNotNull(pluginMetaData);
        Assert.assertNotNull(pluginConfiguration);

        // Get an existing parameter value
        final String value = pluginConfiguration
                .getParameterValue(pluginConfiguration.getParameters().get(0).getName());
        Assert.assertNotNull(value);

        // Get an unknown parameter value
        final String unknowValue = pluginConfiguration.getParameterValue("unknon");
        Assert.assertNull(unknowValue);

    }

    @Test
    public void pluginConfigurationWithoutParameters() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithoutParameters();
        Assert.assertNotNull(aPluginConfiguration);
    }

    /**
     * Test all setter/getter of {@link PluginConfiguration}
     */
    @Test
    public void pluginConfigurationGetSetattributes() {
        final PluginConfiguration aPluginConfiguration = new PluginConfiguration();
        final long anId = 1234567;
        aPluginConfiguration.setId(new Long(anId));
        final Boolean isActive = Boolean.TRUE;
        aPluginConfiguration.setIsActive(isActive);
        final String aLabel = "a label";
        aPluginConfiguration.setLabel(aLabel);
        final String aPluginId = "a plugin id";
        aPluginConfiguration.setPluginId(aPluginId);
        final int priorityOrder = 157;
        aPluginConfiguration.setPriorityOrder(priorityOrder);
        final String versionNumber = "a version number";
        aPluginConfiguration.setVersion(versionNumber);

        Assert.assertEquals(aPluginConfiguration.getId().longValue(), anId);
        Assert.assertEquals(aPluginConfiguration.getIsActive(), isActive);
        Assert.assertEquals(aPluginConfiguration.getLabel(), aLabel);
        Assert.assertEquals(aPluginConfiguration.getPluginId(), aPluginId);
        Assert.assertEquals(aPluginConfiguration.getPriorityOrder().intValue(), priorityOrder);
        Assert.assertEquals(aPluginConfiguration.getVersion(), versionNumber);
    }

    @Test
    public void pluginConfigurationParameters() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        final String parameterName = "paramWithPluginConf";
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameterPluginConfiguration(parameterName, aPluginConfiguration)
                .addParameter("paramIdentifier", BLUE).getParameters();

        final PluginConfiguration pluginConfigurationParameter = new PluginConfiguration();
        pluginConfigurationParameter.setParameters(parameters);

        final PluginConfiguration plgConf = pluginConfigurationParameter.getParameterConfiguration(parameterName);
        
        Assert.assertNotNull(plgConf);
        Assert.assertEquals(plgConf.getPluginId(),aPluginConfiguration.getPluginId());
        Assert.assertEquals(plgConf.getIsActive(),aPluginConfiguration.getIsActive());
        Assert.assertEquals(plgConf.getLabel(),aPluginConfiguration.getLabel());
        Assert.assertEquals(plgConf.getVersion(),aPluginConfiguration.getVersion());
    }

}
