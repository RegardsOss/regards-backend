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

import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.plugins.utils.PluginParameterUtils.PrimitiveObject;

/**
 * Unit testing of {@link PluginUtils}.
 *
 * @author Christophe Mertz
 */
public class PluginUtilsTest extends PluginUtilsTestConstants {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtilsTest.class);

    private static final String PLUGIN_PACKAGE = "fr.cnes.regards.plugins.utils";

    /**
     * Load all plugins
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_PLG_010")
    @Purpose("A plugin is defined with mate-data like, a name, a description, the author, the version, the licence...")
    public void loadPlugins() {
        LOGGER.debug(STARTING + toString());

        // Get all the plugins
        final Map<String, PluginMetaData> maps = PluginUtils.getPlugins(PLUGIN_PACKAGE, Arrays.asList(PLUGIN_PACKAGE));
        Assert.assertNotNull(maps);
        Assert.assertTrue(maps.size() > 1);

        // Get the PluginMetaData of the first plugin
        final PluginMetaData pluginMetaData = maps.get(maps.keySet().stream().findFirst().get());
        Assert.assertNotNull(pluginMetaData);

        Assert.assertNotNull(pluginMetaData.getPluginId());
        Assert.assertNotNull(pluginMetaData.getUrl());
        Assert.assertNotNull(pluginMetaData.getDescription());
        Assert.assertNotNull(pluginMetaData.getAuthor());
        Assert.assertNotNull(pluginMetaData.getContact());
        Assert.assertNotNull(pluginMetaData.getOwner());
        Assert.assertNotNull(pluginMetaData.getVersion());
        Assert.assertNotNull(pluginMetaData.getLicence());

        LOGGER.debug(ENDING + toString());
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     */
    @Test
    public void getSamplePlugin() {
        SamplePlugin samplePlugin = null;

        LOGGER.debug(STARTING + toString());

        /*
         * Set all parameters
         */
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameter(SamplePlugin.SUFFIXE, "chris_test_1").getParameters();
        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE));

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertEquals(Integer.parseInt(PluginUtilsTest.TROIS)
                * (Integer.parseInt(PluginUtilsTest.QUATRE) + Integer.parseInt(PluginUtilsTest.CINQ)),
                            samplePlugin.add(Integer.parseInt(PluginUtilsTest.QUATRE),
                                             Integer.parseInt(PluginUtilsTest.CINQ)));
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.HELLO));
        LOGGER.debug(ENDING + toString());
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     */
    @Test
    public void getSamplePluginWithOneDynamicParameter() {
        SamplePlugin samplePlugin = null;

        LOGGER.debug(STARTING + toString());

        /*
         * Set all parameters
         */
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameterDynamic(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameter(SamplePlugin.SUFFIXE, "suffix").getParameters();
        // init a dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build().addParameter(SamplePlugin.COEFF, "-1")
                .getParameters().stream().findAny().get();

        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE),
                                             aDynamicPlgParam);

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(0 > samplePlugin.add(Integer.parseInt(PluginUtilsTest.QUATRE),
                                               Integer.parseInt(PluginUtilsTest.CINQ)));
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.HELLO));
        LOGGER.debug(ENDING + toString());
    }

    @Test
    public void getSamplePluginDynamicParameterNull() {
        SamplePlugin samplePlugin = null;

        LOGGER.debug(STARTING + toString());

        /*
         * Set all parameters
         */
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameterDynamic(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameter(SamplePlugin.SUFFIXE, "a suffix").getParameters();
        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE));

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(0 < samplePlugin.add(Integer.parseInt(PluginUtilsTest.QUATRE),
                                               Integer.parseInt(PluginUtilsTest.CINQ)));
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.HELLO));
        LOGGER.debug(ENDING + toString());
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     */
    @Test
    public void getSamplePluginWithOneDynamicParameterWithValues() {
        SamplePlugin samplePlugin = null;

        LOGGER.debug(STARTING + toString());

        /*
         * Set all parameters
         */
        final List<String> dynamicValues = Arrays.asList(PluginUtilsTest.RED, PluginUtilsTest.BLUE,
                                                         PluginUtilsTest.GREEN);

        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameterDynamic(SamplePlugin.SUFFIXE, PluginUtilsTest.RED, dynamicValues).getParameters();
        // init a dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                .addParameter(SamplePlugin.SUFFIXE, PluginUtilsTest.BLUE).getParameters().stream().findAny().get();

        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE),
                                             aDynamicPlgParam);

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.BLUE));
        LOGGER.debug(ENDING + toString());
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     */
    @Test
    public void getSamplePluginWithoutDynamicParameterWithValues() {
        SamplePlugin samplePlugin = null;

        LOGGER.debug(STARTING + toString());

        /*
         * Set all parameters
         */
        final List<String> dynamicValues = Arrays.asList(PluginUtilsTest.RED, PluginUtilsTest.BLUE,
                                                         PluginUtilsTest.GREEN);
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameterDynamic(SamplePlugin.SUFFIXE, PluginUtilsTest.RED, dynamicValues).getParameters();
        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE));

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.RED));
        LOGGER.debug(ENDING + toString());
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters @ An error occurs
     */
    @Test(expected = PluginUtilsRuntimeException.class)
    public void getSamplePluginWithUnknownDynamicParameterWithValues() {
        LOGGER.debug(STARTING + toString());

        /*
         * Set all parameters
         */
        final List<String> dynamicValues = Arrays.asList(PluginUtilsTest.RED, PluginUtilsTest.BLUE,
                                                         PluginUtilsTest.GREEN);
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameterDynamic(SamplePlugin.SUFFIXE, PluginUtilsTest.RED, dynamicValues).getParameters();

        // init a dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                .addParameter(SamplePlugin.SUFFIXE, PluginUtilsTest.CINQ).getParameters().stream().findAny().get();

        // instantiate plugin
        PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE), aDynamicPlgParam);
    }

    /**
     * Unable to get {@link SamplePlugin} an Integer parameter is missing @ An error occurs
     */
    @Test(expected = PluginUtilsRuntimeException.class)
    public void getSamplePluginMissingCoeffParameter() {
        LOGGER.debug(STARTING + toString());

        /*
         * Set parameters : Missing coeff parameter
         */
        final List<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> parameters = PluginParametersFactory
                .build().addParameter(SamplePlugin.ACTIVE, "false").addParameter(SamplePlugin.SUFFIXE, "chris_test_3")
                .getParameters();

        // instantiate plugin
        PluginUtils.getPlugin(parameters, SamplePlugin.class,
                              Arrays.asList("fr.cnes.regards.plugins.utils.plugintypes"));
    }

    /**
     * Unable to get {@link SamplePlugin} an Integer parameter is missing @ An error occurs
     */
    @Test(expected = PluginUtilsRuntimeException.class)
    public void getSamplePluginWithErrorInitMethod() {
        LOGGER.debug(STARTING + toString());

        /*
         * Set parameters
         */
        final List<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> parameters = PluginParametersFactory
                .build().addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.SUFFIXE, "chris_test_4")
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.CINQ).getParameters();

        // instantiate plugin
        PluginUtils.getPlugin(parameters, SampleErrorPlugin.class, Arrays.asList(PLUGIN_PACKAGE));
    }

    @Test
    public void testPluginParameterEnum() {
        PrimitiveObject.valueOf(PrimitiveObject.BOOLEAN.name());
        PrimitiveObject.valueOf(PrimitiveObject.INT.name());
        PrimitiveObject.valueOf(PrimitiveObject.STRING.name());
        PrimitiveObject.valueOf(PrimitiveObject.SHORT.name());
        PrimitiveObject.valueOf(PrimitiveObject.FLOAT.name());
        PrimitiveObject.valueOf(PrimitiveObject.LONG.name());
        PrimitiveObject.valueOf(PrimitiveObject.DOUBLE.name());
        PrimitiveObject.valueOf(PrimitiveObject.BYTE.name());
    }

}
