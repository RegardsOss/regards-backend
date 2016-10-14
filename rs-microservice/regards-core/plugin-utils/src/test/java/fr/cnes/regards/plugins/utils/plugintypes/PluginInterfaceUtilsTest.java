/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.plugintypes;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.plugins.utils.PluginInterfaceUtils;
import fr.cnes.regards.plugins.utils.PluginUtilsWrapper;
import fr.cnes.regards.plugins.utils.PluginUtilsException;
import fr.cnes.regards.plugins.utils.PluginUtilsTestConstants;

/**
 * PluginInterfaceUtilsTest
 * 
 * @author cmertz
 *
 */
public final class PluginInterfaceUtilsTest extends PluginUtilsTestConstants {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginInterfaceUtilsTest.class);

    /**
     * Load all plugins
     */
    @Test
    public void loadPluginsInterface() {
        LOGGER.debug("Starting " + this.toString());
        // Get all the plugin interfaces
        final List<String> pluginInterfaces = PluginInterfaceUtils
                .getInterfaces("fr.cnes.regards.plugins.utils.plugintypes");
        Assert.assertNotNull(pluginInterfaces);
        pluginInterfaces.stream().forEach(s -> LOGGER.info(s));
        Assert.assertTrue(pluginInterfaces.size() > 0);
        LOGGER.debug("Ending " + this.toString());
    }

    /**
     * Get a {@link ComplexPlugin} with a specific parameters
     */
    @Test
    public void getComplexPlugin() {
        final ComplexPlugin complexPlugin;
        LOGGER.debug("Starting " + this.toString());
        /*
         * Set all parameters
         */
        try {
            /*
             * Get the configuration for the Plugin parameter (ie the child)
             */
            final List<PluginParameter> interfaceParameters = PluginParametersFactory.build()
                    .addParameter(AParameterPluginImplementation.LONG_PARAM, PluginInterfaceUtilsTest.LONG_STR_VALUE)
                    .getParameters();
            final PluginConfiguration pluginConfigurationInterface = PluginUtilsWrapper
                    .getPluginConfiguration(interfaceParameters, AParameterPluginImplementation.class);
            Assert.assertNotNull(pluginConfigurationInterface);

            /*
             * Get the configuration for the complex Plugin (ie the parent)
             */
            final List<PluginParameter> complexParameters = PluginParametersFactory.build()
                    .addParameterPluginConfiguration(ComplexPlugin.PLUGIN_PARAM, pluginConfigurationInterface)
                    .addParameter(ComplexPlugin.ACTIVE, TRUE)
                    .addParameter(ComplexPlugin.COEFF, PluginInterfaceUtilsTest.CINQ).getParameters();

            /*
             * Instantiate the parent plugin
             */
            complexPlugin = PluginUtilsWrapper.getPlugin(complexParameters, ComplexPlugin.class);
            Assert.assertNotNull(complexPlugin);

            Assert.assertTrue(complexPlugin.add(Integer.parseInt(PluginInterfaceUtilsTest.CINQ),
                                                Integer.parseInt(PluginInterfaceUtilsTest.QUATRE)) > 0);
            Assert.assertTrue(complexPlugin.echo(PluginInterfaceUtilsTest.HELLO_WORLD)
                    .contains(PluginInterfaceUtilsTest.HELLO_WORLD));

            LOGGER.info("plugin parameter:" + complexPlugin.echoPluginParameter());

        } catch (final PluginUtilsException e) {
            LOGGER.error(e.getMessage());
            Assert.assertTrue(false);
        }
        LOGGER.debug("Ending " + this.toString());
    }

    /**
     * 
     */
    @Test
    public void incompatibleInterfaceError() {
        ComplexErrorPlugin complexErrorPlugin = null;
        LOGGER.debug("Starting " + this.toString());
        /*
         * Set all parameters
         */
        try {

            final List<PluginParameter> complexParameters = PluginParametersFactory.build()
                    .addParameter(ComplexErrorPlugin.ACTIVE, TRUE)
                    .addParameter(ComplexErrorPlugin.COEFF, PluginInterfaceUtilsTest.CINQ)
                    .addParameter(ComplexErrorPlugin.PLUGIN_PARAM, "coucou").getParameters();

            // instantiate plugin
            complexErrorPlugin = PluginUtilsWrapper.getPlugin(complexParameters, ComplexErrorPlugin.class);
            Assert.assertNotNull(complexErrorPlugin);

        } catch (final PluginUtilsException e) {
            LOGGER.error(e.getMessage());
            Assert.assertTrue(true);
        }
        LOGGER.debug("Ending " + this.toString());
    }

    /**
     * 
     */
    @Test
    public void incompatibleParameterError() {
        ComplexErrorPlugin complexErrorPlugin = null;
        LOGGER.debug("Starting " + this.toString());
        /*
         * Set all parameters
         */
        try {

            final List<PluginParameter> complexParameters = PluginParametersFactory.build()
                    .addParameter(ComplexErrorPlugin.ACTIVE, TRUE).addParameter(ComplexErrorPlugin.COEFF, "allo")
                    .addParameter(ComplexErrorPlugin.PLUGIN_PARAM, "lorem ipsum").getParameters();

            // instantiate plugin
            complexErrorPlugin = PluginUtilsWrapper.getPlugin(complexParameters, ComplexErrorPlugin.class);
            Assert.assertNotNull(complexErrorPlugin);

        } catch (final PluginUtilsException e) {
            LOGGER.error(e.getMessage());
            Assert.assertTrue(true);
        }
        LOGGER.debug("Ending " + this.toString());
    }

}
