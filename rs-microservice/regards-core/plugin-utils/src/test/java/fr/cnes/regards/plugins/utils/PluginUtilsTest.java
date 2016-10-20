/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils;

import java.util.Arrays;
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
public class PluginUtilsTest extends PluginUtilsTestConstants {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtilsTest.class);

    /**
     * Load all plugins
     */
    @Test
    public void loadPlugins() {
        LOGGER.debug(STARTING + this.toString());

        try {
            // Get all the plugins
            final Map<String, PluginMetaData> maps = PluginUtils.getPlugins("fr.cnes.regards.plugins.utils");
            Assert.assertNotNull(maps);
            Assert.assertTrue(maps.size() > 1);

            // Get the PluginMetaData of the first plugin
            final PluginMetaData pluginMetaData = maps.get(maps.keySet().stream().findFirst().get());
            Assert.assertNotNull(pluginMetaData);

            // Log the parameters of the first plugin
            pluginMetaData.getParameters().stream().forEach(s -> LOGGER.info(s));
        } catch (PluginUtilsException e) {
            Assert.fail();
        }
        LOGGER.debug(ENDING + this.toString());
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     */
    @Test
    public void getSamplePlugin() {
        SamplePlugin samplePlugin = null;

        LOGGER.debug(STARTING + this.toString());

        /*
         * Set all parameters
         */
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameter(SamplePlugin.SUFFIXE, "chris_test_1").getParameters();
        try {
            // instantiate plugin
            samplePlugin = PluginUtilsWrapper.getPlugin(parameters, SamplePlugin.class);
        } catch (final PluginUtilsException e) {
            Assert.fail();
        }
        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertEquals(Integer.parseInt(PluginUtilsTest.TROIS)
                * (Integer.parseInt(PluginUtilsTest.QUATRE) + Integer.parseInt(PluginUtilsTest.CINQ)),
                            samplePlugin.add(Integer.parseInt(PluginUtilsTest.QUATRE),
                                             Integer.parseInt(PluginUtilsTest.CINQ)));
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.HELLO));
        LOGGER.debug(ENDING + this.toString());
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     */
    @Test
    public void getSamplePluginWithOneDynamicParameter() {
        SamplePlugin samplePlugin = null;

        LOGGER.debug(STARTING + this.toString());

        /*
         * Set all parameters
         */
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameterDynamic(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameter(SamplePlugin.SUFFIXE, "chris_test_2").getParameters();
        try {
            // init a dynamic parameter
            final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                    .addParameter(SamplePlugin.COEFF, "-1").getParameters().stream().findAny().get();

            // instantiate plugin
            samplePlugin = PluginUtilsWrapper.getPlugin(parameters, SamplePlugin.class, aDynamicPlgParam);
        } catch (final PluginUtilsException e) {
            Assert.fail();
        }
        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(0 > samplePlugin.add(Integer.parseInt(PluginUtilsTest.QUATRE),
                                               Integer.parseInt(PluginUtilsTest.CINQ)));
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.HELLO));
        LOGGER.debug(ENDING + this.toString());
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     */
    @Test
    public void getSamplePluginWithOneDynamicParameterWithValues() {
        SamplePlugin samplePlugin = null;

        LOGGER.debug(STARTING + this.toString());

        /*
         * Set all parameters
         */
        final List<String> dynamicValues = Arrays.asList(PluginUtilsTest.RED, PluginUtilsTest.BLUE,
                                                         PluginUtilsTest.GREEN);

        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameterDynamic(SamplePlugin.SUFFIXE, PluginUtilsTest.RED, dynamicValues).getParameters();
        try {
            // init a dynamic parameter
            final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                    .addParameter(SamplePlugin.SUFFIXE, PluginUtilsTest.BLUE).getParameters().stream().findAny().get();

            // instantiate plugin
            samplePlugin = PluginUtilsWrapper.getPlugin(parameters, SamplePlugin.class, aDynamicPlgParam);
        } catch (final PluginUtilsException e) {
            Assert.fail();
        }
        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.BLUE));
        LOGGER.debug(ENDING + this.toString());
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     */
    @Test
    public void getSamplePluginWithoutDynamicParameterWithValues() {
        SamplePlugin samplePlugin = null;

        LOGGER.debug(STARTING + this.toString());

        /*
         * Set all parameters
         */
        final List<String> dynamicValues = Arrays.asList(PluginUtilsTest.RED, PluginUtilsTest.BLUE,
                                                         PluginUtilsTest.GREEN);
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameterDynamic(SamplePlugin.SUFFIXE, PluginUtilsTest.RED, dynamicValues).getParameters();
        try {
            // instantiate plugin
            samplePlugin = PluginUtilsWrapper.getPlugin(parameters, SamplePlugin.class);
        } catch (final PluginUtilsException e) {
            Assert.fail();
        }
        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.RED));
        LOGGER.debug(ENDING + this.toString());
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     * 
     * @throws PluginUtilsException
     *             An error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void getSamplePluginWithUnknownDynamicParameterWithValues() throws PluginUtilsException {
        LOGGER.debug(STARTING + this.toString());

        /*
         * Set all parameters
         */
        List<String> dynamicValues = Arrays.asList(PluginUtilsTest.RED, PluginUtilsTest.BLUE, PluginUtilsTest.GREEN);
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameterDynamic(SamplePlugin.SUFFIXE, PluginUtilsTest.RED, dynamicValues).getParameters();

        // init a dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                .addParameter(SamplePlugin.SUFFIXE, PluginUtilsTest.CINQ).getParameters().stream().findAny().get();

        // instantiate plugin
        PluginUtilsWrapper.getPlugin(parameters, SamplePlugin.class, aDynamicPlgParam);
    }

    /**
     * Unable to get {@link SamplePlugin} an Integer parameter is missing
     * 
     * @throws PluginUtilsException
     *             An error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void getSamplePluginMissingCoeffParameter() throws PluginUtilsException {
        LOGGER.debug(STARTING + this.toString());

        /*
         * Set parameters : Missing coeff parameter
         */
        final List<fr.cnes.regards.modules.plugins.domain.PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, "false").addParameter(SamplePlugin.SUFFIXE, "chris_test_3")
                .getParameters();

        // instantiate plugin
        PluginUtilsWrapper.getPlugin(parameters, SamplePlugin.class);
    }

    /**
     * Unable to get {@link SamplePlugin} an Integer parameter is missing
     * 
     * @throws PluginUtilsException
     *             An error occurs
     */
    @Test(expected = PluginUtilsException.class)
    public void getSamplePluginWithErrorInitMethod() throws PluginUtilsException {
        LOGGER.debug(STARTING + this.toString());

        /*
         * Set parameters
         */
        final List<fr.cnes.regards.modules.plugins.domain.PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.SUFFIXE, "chris_test_4")
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.CINQ).getParameters();

        // instantiate plugin
        PluginUtilsWrapper.getPlugin(parameters, SampleErrorPlugin.class);
    }

}
