/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.plugintypes;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.plugins.utils.PluginInterfaceUtils;
import fr.cnes.regards.plugins.utils.PluginUtils;
import fr.cnes.regards.plugins.utils.PluginUtilsException;
import fr.cnes.regards.plugins.utils.PluginUtilsTestConstants;

/**
 * Unit testing of {@link PluginInterfaceUtils}.
 * 
 * @author Christophe Mertz
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
        LOGGER.debug(STARTING + this.toString());
        // Get all the plugin interfaces
        final List<String> pluginInterfaces = PluginInterfaceUtils
                .getInterfaces(Arrays.asList("fr.cnes.regards.plugins.utils.plugintypes",
                                             "fr.cnes.regards.plugins.utils.bean", "fr.cnes.regards.plugins.utils"));
        Assert.assertNotNull(pluginInterfaces);
        pluginInterfaces.stream().forEach(s -> LOGGER.info(s));
        Assert.assertTrue(pluginInterfaces.size() > 0);
        LOGGER.debug(ENDING + this.toString());
    }

    /**
     * Load plugins in an empty package
     */
    @Test
    public void loadPluginsInterfaceEmpty() {
        LOGGER.debug(STARTING + this.toString());
        // Get all the plugin interfaces
        final List<String> pluginInterfaces = PluginInterfaceUtils
                .getInterfaces("fr.cnes.regards.plugins.utils.plugintypes.empty");
        Assert.assertNotNull(pluginInterfaces);
        Assert.assertTrue(pluginInterfaces.isEmpty());
        LOGGER.debug(ENDING + this.toString());
    }

    /**
     * Load all plugins in several packages
     */
    @Test
    public void loadPluginsInterfaceSeveralPrefix() {
        LOGGER.debug(STARTING + this.toString());
        // Get all the plugin interfaces
        final List<String> pluginInterfaces = PluginInterfaceUtils
                .getInterfaces(Arrays.asList("fr.cnes.regards.plugins.utils.plugintypes"));
        Assert.assertNotNull(pluginInterfaces);
        pluginInterfaces.stream().forEach(s -> LOGGER.info(s));
        Assert.assertTrue(pluginInterfaces.size() > 0);
        LOGGER.debug(ENDING + this.toString());
    }

    /**
     * Load plugin in empty several packages
     */
    @Test
    public void loadNoPluginsInterfaceSeveralPrefix() {
        LOGGER.debug(STARTING + this.toString());
        // Get all the plugin interfaces
        final List<String> pluginInterfaces = PluginInterfaceUtils
                .getInterfaces(Arrays.asList("fr.cnes.regards.plugins.utils.plugintypes.empty",
                                             "fr.cnes.regards.plugins.utils.plugintypes.empty.sub",
                                             "fr.cnes.regards.plugins.utils.plugintypes.empty.sub2"));
        Assert.assertNotNull(pluginInterfaces);
        Assert.assertTrue(pluginInterfaces.isEmpty());
        LOGGER.debug(ENDING + this.toString());
    }

    /**
     * Get a {@link ComplexPlugin} with a specific parameters
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_PLG_020")
    @Purpose("Load a plugin with a plugin type interface parameter.")
    public void getComplexPlugin() {
        final ComplexPlugin complexPlugin;
        LOGGER.debug(STARTING + this.toString());
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
            final PluginConfiguration pluginConfigurationInterface = PluginUtils
                    .getPluginConfiguration(interfaceParameters, AParameterPluginImplementation.class,
                                            Arrays.asList("fr.cnes.regards.plugins.utils.plugintypes"));
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
            complexPlugin = PluginUtils.getPlugin(complexParameters, ComplexPlugin.class,
                                                  Arrays.asList("fr.cnes.regards.plugins.utils.plugintypes"));
            Assert.assertNotNull(complexPlugin);

            Assert.assertTrue(complexPlugin.add(Integer.parseInt(PluginInterfaceUtilsTest.CINQ),
                                                Integer.parseInt(PluginInterfaceUtilsTest.QUATRE)) > 0);
            Assert.assertTrue(complexPlugin.echo(PluginInterfaceUtilsTest.HELLO_WORLD)
                    .contains(PluginInterfaceUtilsTest.HELLO_WORLD));

            LOGGER.info("plugin parameter:" + complexPlugin.echoPluginParameter());

        } catch (final PluginUtilsException e) {
            LOGGER.error(e.getMessage());
            Assert.fail();
        }
        LOGGER.debug(ENDING + this.toString());
    }

    /**
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    @Requirement("REGARDS_DSL_SYS_PLG_020")
    @Purpose("Error to load a plugin from with an incompatible interface parameter.")
    public void incompatibleInterfaceError() throws PluginUtilsException {
        LOGGER.debug(STARTING + this.toString());
        /*
         * Set all parameters
         */

        final List<PluginParameter> complexParameters = PluginParametersFactory.build()
                .addParameter(ComplexErrorPlugin.ACTIVE, TRUE)
                .addParameter(ComplexErrorPlugin.COEFF, PluginInterfaceUtilsTest.CINQ)
                .addParameter(ComplexErrorPlugin.PLUGIN_PARAM, "coucou").getParameters();

        // instantiate plugin
        PluginUtils.getPlugin(complexParameters, ComplexErrorPlugin.class,
                              Arrays.asList("fr.cnes.regards.plugins.utils.plugintypes"));
    }

    /**
     * @throws PluginUtilsException
     *             throw if an error occurs
     */
    @Test(expected = PluginUtilsException.class)
    @Requirement("REGARDS_DSL_SYS_PLG_020")
    @Purpose("Error to load a plugin from with an incompatible interface parameter.")
    public void incompatibleParameterError() throws PluginUtilsException {
        LOGGER.debug(STARTING + this.toString());
        /*
         * Set all parameters
         */

        final List<PluginParameter> complexParameters = PluginParametersFactory.build()
                .addParameter(ComplexErrorPlugin.ACTIVE, TRUE).addParameter(ComplexErrorPlugin.COEFF, "allo")
                .addParameter(ComplexErrorPlugin.PLUGIN_PARAM, "lorem ipsum").getParameters();

        // instantiate plugin
        PluginUtils.getPlugin(complexParameters, ComplexErrorPlugin.class,
                              Arrays.asList("fr.cnes.regards.plugins.utils.plugintypes"));
    }

}
