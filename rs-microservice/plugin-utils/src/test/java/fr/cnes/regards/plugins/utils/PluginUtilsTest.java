/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;

/**
 * PluginUtilsTest
 * 
 * @author cmertz
 *
 */
public class PluginUtilsTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtilsTest.class);

    /**
     * Load all plugins
     */
    @Test
    public void loadPlugins() {
        LOGGER.info("start " + this.toString());
        try {
            // Get all the plugins
            final Map<String, PluginMetaData> maps = AbstractPluginUtils.getPlugins("fr.cnes.regards.plugins.utils");
            Assert.assertNotNull(maps);
            Assert.assertTrue(maps.size() > 1);

            // Get the PluginMetaData of the first plugin
            final PluginMetaData pluginMetaData = maps.get(maps.keySet().stream().findFirst().get());
            Assert.assertNotNull(pluginMetaData);

            // Log the parameters of the first plugin
            pluginMetaData.getParameters().stream().forEach(s -> LOGGER.info(s));
        } catch (PluginUtilsException e) {
            LOGGER.error(e.getMessage());
            Assert.assertTrue(false);
        }
        Assert.assertTrue(true);
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     */
    @Test
    public void getSamplePlugin() {
        SamplePlugin samplePlugin = null;
        /*
         * Set all parameters
         */
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, "true").addParameter(SamplePlugin.COEFF, "5")
                .addParameter(SamplePlugin.SUFFIXE, "chris_test_1").getParameters();
        try {
            // instantiate plugin
            samplePlugin = PluginManagerServiceTest.getPlugin(parameters, SamplePlugin.class);
        } catch (final PluginUtilsException e) {
            LOGGER.error(e.getMessage());
            Assert.assertTrue(true);
        }
        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertEquals(40, samplePlugin.add(5, 3));
        Assert.assertTrue(samplePlugin.echo("hello world").contains("hello"));
    }

    /**
     * Unable to get {@link SamplePlugin} an Integer parameter is missing
     */
    @Test
    public void getSamplePluginMissingCoeffParameter() {
        SamplePlugin samplePlugin = null;
        /*
         * Set parameters : Missing coeff parameter
         */
        final List<fr.cnes.regards.modules.plugins.domain.PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, "false").addParameter(SamplePlugin.SUFFIXE, "chris_test_2")
                .getParameters();
        try {
            // instantiate plugin
            samplePlugin = PluginManagerServiceTest.getPlugin(parameters, SamplePlugin.class);
        } catch (final PluginUtilsException e) {
            LOGGER.error(e.getCause().getMessage());
            Assert.assertTrue(true);
        }
        Assert.assertNull(samplePlugin);
    }

}
