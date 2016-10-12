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
import fr.cnes.regards.plugins.utils.AbstractPluginInterfaceUtils;
import fr.cnes.regards.plugins.utils.PluginManagerServiceTest;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 * PluginInterfaceUtilsTest
 * 
 * @author cmertz
 *
 */
public class PluginInterfaceUtilsTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginInterfaceUtilsTest.class);

    /**
     * TRUE constant {@link String}
     */
    private static final String TRUE = "true";

    /**
     * Load all plugins
     */
    @Test
    public void loadPluginsInterface() {
        LOGGER.debug("Starting " + this.toString());
        // Get all the plugin interfaces
        final List<String> pluginInterfaces = AbstractPluginInterfaceUtils
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
                    .addParameter(ParameterPlugin.LONG_PARAM, "123456789").getParameters();
            final PluginConfiguration pluginConfigurationInterface = PluginManagerServiceTest
                    .getPluginConfiguration(interfaceParameters, ParameterPlugin.class);
            Assert.assertNotNull(pluginConfigurationInterface);

            /*
             * Get the configuration for the complex Plugin (ie the parent)
             */
            final List<PluginParameter> complexParameters = PluginParametersFactory.build()
                    .addParameterPluginConfiguration(ComplexPlugin.PLUGIN_PARAM, pluginConfigurationInterface)
                    .addParameter(ComplexPlugin.ACTIVE, TRUE).addParameter(ComplexPlugin.COEFF, "33").getParameters();

            /*
             * Instantiate the parent plugin
             */
            complexPlugin = PluginManagerServiceTest.getPlugin(complexParameters, ComplexPlugin.class);
            Assert.assertNotNull(complexPlugin);

            Assert.assertTrue(complexPlugin.add(15, 10) > 0);
            final String str = "hello world";
            Assert.assertTrue(complexPlugin.echo(str).contains(str));

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
                    .addParameter(ComplexErrorPlugin.ACTIVE, TRUE).addParameter(ComplexErrorPlugin.COEFF, "5")
                    .addParameter(ComplexErrorPlugin.PLUGIN_PARAM, "coucou").getParameters();

            // instantiate plugin
            complexErrorPlugin = PluginManagerServiceTest.getPlugin(complexParameters, ComplexErrorPlugin.class);
            Assert.assertNotNull(complexErrorPlugin);

        } catch (final PluginUtilsException e) {
            LOGGER.error(e.getMessage());
            Assert.assertTrue(true);
        }
        LOGGER.debug("Ending " + this.toString());
    }

}
