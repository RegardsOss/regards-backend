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
public class PluginUtilsTest extends AbstractPluginUtilsConstants {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtilsTest.class);

    /**
     * Load all plugins
     */
    @Test
    public void loadPlugins() {
        LOGGER.debug("Starting " + this.toString());
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
        LOGGER.debug("Ending " + this.toString());
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     */
    @Test
    public void getSamplePlugin() {
        SamplePlugin samplePlugin = null;
        LOGGER.debug("Starting " + this.toString());
        /*
         * Set all parameters
         */
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameter(SamplePlugin.SUFFIXE, "chris_test_1").getParameters();
        try {
            // instantiate plugin
            samplePlugin = PluginManagerServiceTest.getPlugin(parameters, SamplePlugin.class);
        } catch (final PluginUtilsException e) {
            LOGGER.error(e.getMessage());
            Assert.assertTrue(false);
        }
        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertEquals(Integer.parseInt(PluginUtilsTest.TROIS)
                * (Integer.parseInt(PluginUtilsTest.QUATRE) + Integer.parseInt(PluginUtilsTest.CINQ)),
                            samplePlugin.add(Integer.parseInt(PluginUtilsTest.QUATRE),
                                             Integer.parseInt(PluginUtilsTest.CINQ)));
        Assert.assertTrue(samplePlugin.echo("hello world").contains("hello"));
        LOGGER.debug("Ending " + this.toString());
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     */
    @Test
    public void getSamplePluginWithOneDynamicParameter() {
        SamplePlugin samplePlugin = null;
        LOGGER.debug("Starting " + this.toString());
        /*
         * Set all parameters
         */
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameterDynamic(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameter(SamplePlugin.SUFFIXE, "chris_test_1").getParameters();
        try {
            // init a dynamic parameter
            final PluginParameter aDynamicPluginParam = PluginParametersFactory.build()
                    .addParameter(SamplePlugin.COEFF, "-1").getParameters().stream().findAny().get();

            // instantiate plugin
            samplePlugin = PluginManagerServiceTest.getPlugin(parameters, SamplePlugin.class, aDynamicPluginParam);
        } catch (final PluginUtilsException e) {
            LOGGER.error(e.getMessage());
            Assert.assertTrue(false);
        }
        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(0 > samplePlugin.add(Integer.parseInt(PluginUtilsTest.QUATRE),
                                               Integer.parseInt(PluginUtilsTest.CINQ)));
        Assert.assertTrue(samplePlugin.echo("hello world").contains("hello"));
        LOGGER.debug("Ending " + this.toString());
    }

    /**
     * Unable to get {@link SamplePlugin} an Integer parameter is missing
     */
    @Test
    public void getSamplePluginMissingCoeffParameter() {
        SamplePlugin samplePlugin = null;
        LOGGER.debug("Starting " + this.toString());
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
        LOGGER.debug("Ending " + this.toString());
    }

    /**
     * Unable to get {@link SamplePlugin} an Integer parameter is missing
     */
    @Test
    public void getSamplePluginWithErrorInitMethod() {
        SampleErrorPlugin samplePlugin = null;
        LOGGER.debug("Starting " + this.toString());
        /*
         * Set parameters
         */
        final List<fr.cnes.regards.modules.plugins.domain.PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SampleErrorPlugin.SUFFIXE, "chris_test_3")
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.CINQ).getParameters();
        try {
            // instantiate plugin
            samplePlugin = PluginManagerServiceTest.getPlugin(parameters, SampleErrorPlugin.class);
        } catch (final PluginUtilsException e) {
            LOGGER.error(e.getCause().getMessage());
            Assert.assertTrue(true);
        }
        Assert.assertNull(samplePlugin);
        LOGGER.debug("Ending " + this.toString());
    }

}
