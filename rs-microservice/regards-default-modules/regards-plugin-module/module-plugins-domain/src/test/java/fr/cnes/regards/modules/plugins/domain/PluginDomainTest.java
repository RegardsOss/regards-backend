package fr.cnes.regards.modules.plugins.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author Christophe Mertz
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
        Assert.assertNotNull(pluginConfiguration.getPluginClassName());

        // Get an existing parameter value
        final String value = pluginConfiguration
                .getParameterValue(pluginConfiguration.getParameters().get(0).getName());
        Assert.assertNotNull(value);

        // Get an unknown parameter value
        final String unknowValue = pluginConfiguration.getParameterValue("unknon");
        Assert.assertNull(unknowValue);

        pluginConfiguration.logParams();

    }

    @Test
    public void pluginConfigurationWithoutParameters() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithoutParameters();
        Assert.assertNotNull(aPluginConfiguration);

        PluginParametersFactory.build().addParameterDynamic("param", RED, new ArrayList<String>()).getParameters();
    }

    /**
     * Test all setter/getter of {@link PluginConfiguration}
     */
    @Test
    public void pluginConfigurationGetSetAttributes() {
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
        Assert.assertEquals(aPluginConfiguration.isActive(), isActive);
        Assert.assertEquals(aPluginConfiguration.getLabel(), aLabel);
        Assert.assertEquals(aPluginConfiguration.getPluginId(), aPluginId);
        Assert.assertEquals(aPluginConfiguration.getPriorityOrder().intValue(), priorityOrder);
        Assert.assertEquals(aPluginConfiguration.getVersion(), versionNumber);
    }

    @Test
    public void pluginConfigurationParameters() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        final String parameterName = "paramWithPluginConf1";
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameterPluginConfiguration(parameterName, aPluginConfiguration)
                .addParameter("paramIdentifier1", BLUE).getParameters();
        parameters.get(0).setId(AN_ID);

        final PluginConfiguration pluginConfigurationParameter = new PluginConfiguration();
        pluginConfigurationParameter.setParameters(parameters);

        final PluginConfiguration plgConf = pluginConfigurationParameter.getParameterConfiguration(parameterName);

        Assert.assertNotNull(plgConf);
        Assert.assertEquals(plgConf.getPluginId(), aPluginConfiguration.getPluginId());
        Assert.assertEquals(plgConf.isActive(), aPluginConfiguration.isActive());
        Assert.assertEquals(plgConf.getLabel(), aPluginConfiguration.getLabel());
        Assert.assertEquals(plgConf.getVersion(), aPluginConfiguration.getVersion());
        Assert.assertEquals(plgConf.getParameters().get(0).getId(),
                            aPluginConfiguration.getParameters().get(0).getId());
        
        plgConf.logParams();
    }

    @Test
    public void pluginConfigurationGetUnknowParameterName() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        final String parameterName = "paramWithPluginConf2";
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameterPluginConfiguration(parameterName, aPluginConfiguration)
                .addParameter("paramIdentifier2", BLUE).getParameters();
        parameters.get(0).setId(AN_ID);

        final PluginConfiguration pluginConfigurationParameter = new PluginConfiguration();
        pluginConfigurationParameter.setParameters(parameters);

        final PluginConfiguration plgConf = pluginConfigurationParameter
                .getParameterConfiguration("unknown-parameter-name");

        Assert.assertNull(plgConf);
    }

    @Test
    public void pluginConfigurationGetUnknowParameterName2() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithoutParameters();

        final PluginConfiguration plgConf = aPluginConfiguration
                .getParameterConfiguration("other-unknown-parameter-name");

        Assert.assertNull(plgConf);
    }

    /**
     * Test domain {@link PluginMetaData}
     */
    @Test
    public void pluginMetaData() {
        final PluginMetaData plgMetaData = getPluginMetaData();
        final String anAuthor = GREEN;
        plgMetaData.setAuthor(anAuthor);
        final String aDescription = USERROLE + BLUE + RED;
        plgMetaData.setDescription(aDescription);
        final List<String> parameters = Arrays.asList(RED, BLUE, GREEN);
        plgMetaData.setParameters(parameters);

        Assert.assertEquals(anAuthor, plgMetaData.getAuthor());
        Assert.assertEquals(aDescription, plgMetaData.getDescription());
        Assert.assertEquals(parameters, plgMetaData.getParameters());
    }

    /**
     * Test domain {@link PluginParameter}
     */
    @Test
    public void pluginParameter() {
        final PluginParameter plgParam = new PluginParameter();
        plgParam.setName(RED);
        plgParam.setValue(GREEN);
        plgParam.setIsDynamic(Boolean.FALSE);

        Assert.assertEquals(RED, plgParam.getName());
        Assert.assertEquals(GREEN, plgParam.getValue());
        Assert.assertEquals(false, plgParam.isDynamic().booleanValue());

        // test dynamics==null
        Assert.assertEquals(plgParam.getDynamicsValuesAsString().size(), 0);

        // test dynamics!=null && dynamics.isEmpty
        final List<PluginDynamicValue> dynValues = new ArrayList<>();
        plgParam.setDynamicsValues(dynValues);
        Assert.assertEquals(plgParam.getDynamicsValuesAsString().size(), dynValues.size());

        dynValues.add(new PluginDynamicValue(BLUE));
        dynValues.add(new PluginDynamicValue(GREEN));
        final PluginDynamicValue plgDynValue = new PluginDynamicValue();
        plgDynValue.setValue(BLUE);
        plgDynValue.setId(AN_ID);
        dynValues.add(plgDynValue);

        plgParam.setDynamicsValues(dynValues);

        Assert.assertEquals(plgParam.getDynamicsValues().size(), dynValues.size());
        Assert.assertEquals(plgParam.getDynamicsValuesAsString().size(), dynValues.size());
    }

}
