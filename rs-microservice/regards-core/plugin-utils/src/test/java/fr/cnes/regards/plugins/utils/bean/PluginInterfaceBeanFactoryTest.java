/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.bean;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.plugins.utils.PluginInterfaceUtils;
import fr.cnes.regards.plugins.utils.PluginUtils;
import fr.cnes.regards.plugins.utils.PluginUtilsException;
import fr.cnes.regards.plugins.utils.PluginUtilsTestConstants;
import fr.cnes.regards.plugins.utils.SamplePlugin;

/**
 * Unit testing of {@link PluginInterfaceUtils}.
 * 
 * @author Christophe Mertz
 *
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@RunWith(SpringRunner.class)
@PropertySource("application-test.properties")
@ComponentScan
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
        try {
            // instantiate plugin
            samplePlugin = PluginUtils.getPlugin(parameters, SampleBeanFactoryPlugin.class, pluginUtilsBean);
        } catch (final PluginUtilsException e) {
            Assert.fail();
        }
        Assert.assertNotNull(samplePlugin);
        Assert.assertTrue(samplePlugin.echo("Toulouse").contains("Toulouse"));
        LOGGER.info(samplePlugin.echo("Toulouse"));
    }

}
