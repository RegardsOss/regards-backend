package fr.cnes.regards.framework.utils.plugins.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameterType;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameterType.ParamType;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.AbstractPluginParam;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;

/**
 * @author Christophe Mertz
 */
public class PluginDomainTest extends PluginDomainUtility {

    /**
     * Test the {@link PluginConfiguration} creation.
     */
    @Test
    public void pluginConfigurationWithDynParameters() {
        PluginConfiguration pluginConfiguration = getPluginConfigurationWithDynamicParameter();
        PluginMetaData pluginMetaData = getPluginMetaData();
        Assert.assertNotNull(pluginMetaData);
        Assert.assertNotNull(pluginConfiguration);
        Assert.assertNotNull(pluginConfiguration.getPluginClassName());

        // Get an existing parameter value
        Assert.assertNotEquals(0, pluginConfiguration.getParameters().size());
        Assert.assertNotNull(pluginConfiguration.getParameters().stream().findFirst().get());
        Assert.assertNotNull(pluginConfiguration.getParameters().stream().findFirst().get().getName());

        // Get an unknown parameter value
        String unknowValue = pluginConfiguration.getParameterValue("unknon");
        Assert.assertNull(unknowValue);
    }

    @Test
    public void pluginConfigurationWithoutParameters() {
        PluginConfiguration aPluginConfiguration = getPluginConfigurationWithoutParameters();
        Assert.assertNotNull(aPluginConfiguration);

        PluginParametersFactory.build().addDynamicParameter("param", RED, new ArrayList<>()).getParameters();
    }

    /**
     * Test all setter/getter of {@link PluginConfiguration}
     */
    @Test
    public void pluginConfigurationGetSetAttributes() {
        PluginConfiguration aPluginConfiguration = new PluginConfiguration();
        long anId = 1234567;
        aPluginConfiguration.setId(anId);
        Boolean isActive = Boolean.TRUE;
        aPluginConfiguration.setIsActive(isActive);
        String aLabel = "a label";
        aPluginConfiguration.setLabel(aLabel);
        String aPluginId = "a plugin id";
        aPluginConfiguration.setPluginId(aPluginId);
        int priorityOrder = 157;
        aPluginConfiguration.setPriorityOrder(priorityOrder);
        String versionNumber = "a version number";
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
        PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        String parameterName = "paramWithPluginConf1";
        Set<AbstractPluginParam> parameters = PluginParametersFactory.build()
                .addPluginConfiguration(parameterName, aPluginConfiguration).addParameter("paramIdentifier1", BLUE)
                .getParameters();
        parameters.stream().findFirst().get().setId(AN_ID);

        PluginConfiguration pluginConfigurationParameter = new PluginConfiguration();
        pluginConfigurationParameter.setParameters(parameters);

        PluginConfiguration plgConf = pluginConfigurationParameter.getParameterConfiguration(parameterName);

        Assert.assertNotNull(plgConf);
        Assert.assertEquals(plgConf.getPluginId(), aPluginConfiguration.getPluginId());
        Assert.assertEquals(plgConf.isActive(), aPluginConfiguration.isActive());
        Assert.assertEquals(plgConf.getLabel(), aPluginConfiguration.getLabel());
        Assert.assertEquals(plgConf.getVersion(), aPluginConfiguration.getVersion());
        Assert.assertEquals(plgConf.getParameters().stream().findFirst().get().getId(),
                            aPluginConfiguration.getParameters().stream().findFirst().get().getId());
    }

    @Test
    public void pluginParametersFactory() {
        PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        int n = aPluginConfiguration.getParameters().size();
        Set<AbstractPluginParam> parameters = PluginParametersFactory.build(aPluginConfiguration.getParameters())
                .addPluginConfiguration("paramWithPluginConf1", aPluginConfiguration)
                .addParameter("paramIdentifier1", BLUE)
                .removeParameter(aPluginConfiguration.getParameters().stream().findFirst().get()).getParameters();
        Assert.assertEquals(n + 1, parameters.size());
    }

    @Test
    public void pluginConfigurationGetUnknowParameterName() {
        PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        String parameterName = "paramWithPluginConf2";
        Set<AbstractPluginParam> parameters = PluginParametersFactory.build()
                .addPluginConfiguration(parameterName, aPluginConfiguration).addParameter("paramIdentifier2", BLUE)
                .getParameters();
        parameters.stream().findFirst().get().setId(AN_ID);

        PluginConfiguration pluginConfigurationParameter = new PluginConfiguration();
        pluginConfigurationParameter.setParameters(parameters);

        PluginConfiguration plgConf = pluginConfigurationParameter.getParameterConfiguration("unknown-parameter-name");

        Assert.assertNull(plgConf);
    }

    @Test
    public void pluginConfigurationGetUnknowParameterName2() {
        PluginConfiguration aPluginConfiguration = getPluginConfigurationWithoutParameters();

        PluginConfiguration plgConf = aPluginConfiguration.getParameterConfiguration("other-unknown-parameter-name");

        Assert.assertNull(plgConf);
    }

    /**
     * Test domain {@link PluginMetaData}
     */
    @Test
    public void pluginMetaData() {
        PluginMetaData plgMetaData = getPluginMetaData();
        String anAuthor = GREEN;
        plgMetaData.setAuthor(anAuthor);
        String aDescription = USERROLE + BLUE + RED;
        plgMetaData.setDescription(aDescription);
        List<PluginParameterType> parameters = Arrays.asList(PluginParameterType.create(RED, "red", null, String.class,
                                                                                        ParamType.PRIMITIVE, false,
                                                                                        false, false),
                                                             PluginParameterType
                                                                     .create(BLUE, "blue", null, String.class,
                                                                             ParamType.PRIMITIVE, false, false, false),
                                                             PluginParameterType
                                                                     .create(GREEN, "green", null, String.class,
                                                                             ParamType.PLUGIN, false, false, false));

        plgMetaData.setParameters(parameters);

        Assert.assertEquals(anAuthor, plgMetaData.getAuthor());
        Assert.assertEquals(aDescription, plgMetaData.getDescription());
        Assert.assertEquals(parameters, plgMetaData.getParameters());
    }
}
