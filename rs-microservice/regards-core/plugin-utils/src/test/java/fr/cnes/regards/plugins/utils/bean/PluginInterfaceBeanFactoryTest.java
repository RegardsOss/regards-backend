/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.bean;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.plugins.utils.PluginInterfaceUtils;
import fr.cnes.regards.plugins.utils.PluginUtils;
import fr.cnes.regards.plugins.utils.PluginUtilsTestConstants;

/**
 * Unit testing of {@link PluginInterfaceUtils}.
 *
 * @author Christophe Mertz
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestConfiguration.class })
public final class PluginInterfaceBeanFactoryTest extends PluginUtilsTestConstants {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginInterfaceBeanFactoryTest.class);

    @Autowired
    private PluginUtilsBean pluginUtilsBean;

    @Autowired
    private ISampleBeanService sampleBeanService;

    @Test
    public void getBean() {
        Assert.assertNotNull(pluginUtilsBean);
        Assert.assertNotNull(sampleBeanService);

        final ISampleBeanService asBeanService = pluginUtilsBean.getBeanFactory().getBean((ISampleBeanService.class));
        Assert.assertNotNull(asBeanService);
    }

    /**
     * Load a plugins
     */
    @Test
    public void loadPlugin() {
        SampleBeanFactoryPlugin samplePlugin = null;
        Assert.assertNotNull(pluginUtilsBean);
        Assert.assertNotNull(sampleBeanService);

        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SampleBeanFactoryPlugin.SUFFIXE, "chris_test_1").getParameters();
        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SampleBeanFactoryPlugin.class, pluginUtilsBean,
                                             Arrays.asList("fr.cnes.regards.plugins.utils.bean"), new HashMap<>());

        Assert.assertNotNull(samplePlugin);
        final String toulouse = "Toulouse";
        Assert.assertTrue(samplePlugin.echo(toulouse).contains(toulouse));
        LOGGER.info(samplePlugin.echo(toulouse));
    }

}
