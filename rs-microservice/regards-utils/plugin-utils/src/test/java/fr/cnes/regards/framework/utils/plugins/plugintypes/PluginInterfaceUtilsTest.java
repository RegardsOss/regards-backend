/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.plugins.utils.plugintypes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.plugins.utils.PluginInterfaceUtils;
import fr.cnes.regards.plugins.utils.PluginUtils;
import fr.cnes.regards.plugins.utils.PluginUtilsRuntimeException;
import fr.cnes.regards.plugins.utils.PluginUtilsTestConstants;

/**
 * Unit testing of {@link PluginInterfaceUtils}.
 *
 * @author Christophe Mertz
 */
public final class PluginInterfaceUtilsTest extends PluginUtilsTestConstants {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginInterfaceUtilsTest.class);

    /**
     * The current plugin package
     */
    private static final String PLUGIN_CURRENT_PACKAGE = "fr.cnes.regards.plugins.utils.plugintypes";

    /**
     * A {@link List} of package
     */
    private static final List<String> PLUGIN_PACKAGES = Arrays
            .asList(PLUGIN_CURRENT_PACKAGE, "fr.cnes.regards.plugins.utils.bean", "fr.cnes.regards.plugins.utils");

    /**
     * A not exiting plugin package
     */
    private static final String PLUGIN_EMPTY_PACKAGE = "fr.cnes.regards.plugins.utils.plugintypes.empty";

    /**
     * A {@link List} of not existing plugin package
     */
    private static final List<String> PLUGIN_EMPTY_PACKAGES = Arrays
            .asList(PLUGIN_EMPTY_PACKAGE, "fr.cnes.regards.plugins.utils.plugintypes.empty.sub",
                    "fr.cnes.regards.plugins.utils.plugintypes.empty.sub2");

    /**
     * Load all plugins
     */
    @Test
    public void loadPluginsInterface() {
        LOGGER.debug(STARTING + toString());
        // Get all the plugin interfaces
        final List<String> pluginInterfaces = PluginInterfaceUtils.getInterfaces(PLUGIN_PACKAGES);
        Assert.assertNotNull(pluginInterfaces);
        pluginInterfaces.stream().forEach(s -> LOGGER.info(s));
        Assert.assertTrue(pluginInterfaces.size() > 0);
        LOGGER.debug(ENDING + toString());
    }

    /**
     * Load plugins in an empty package
     */
    @Test
    public void loadPluginsInterfaceEmpty() {
        LOGGER.debug(STARTING + toString());
        // Get all the plugin interfaces
        final List<String> pluginInterfaces = PluginInterfaceUtils.getInterfaces(PLUGIN_EMPTY_PACKAGE);
        Assert.assertNotNull(pluginInterfaces);
        Assert.assertTrue(pluginInterfaces.isEmpty());
        LOGGER.debug(ENDING + toString());
    }

    /**
     * Load all plugins in several packages
     */
    @Test
    public void loadPluginsInterfaceSeveralPrefix() {
        LOGGER.debug(STARTING + toString());
        // Get all the plugin interfaces
        final List<String> pluginInterfaces = PluginInterfaceUtils.getInterfaces(Arrays.asList(PLUGIN_CURRENT_PACKAGE));
        Assert.assertNotNull(pluginInterfaces);
        pluginInterfaces.stream().forEach(s -> LOGGER.info(s));
        Assert.assertTrue(pluginInterfaces.size() > 0);
        LOGGER.debug(ENDING + toString());
    }

    /**
     * Load plugin in empty several packages
     */
    @Test
    public void loadNoPluginsInterfaceSeveralPrefix() {
        LOGGER.debug(STARTING + toString());
        // Get all the plugin interfaces
        final List<String> pluginInterfaces = PluginInterfaceUtils.getInterfaces(PLUGIN_EMPTY_PACKAGES);
        Assert.assertNotNull(pluginInterfaces);
        Assert.assertTrue(pluginInterfaces.isEmpty());
        LOGGER.debug(ENDING + toString());
    }

    /**
     * Get a {@link ComplexPlugin} with a specific parameters
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_PLG_020")
    @Purpose("Load a plugin with a plugin type interface parameter.")
    public void getComplexPlugin() {
        final ComplexPlugin complexPlugin;
        LOGGER.debug(STARTING + toString());
        /*
         * Set all parameters
         */
        /*
         * Get the configuration for the Plugin parameter (ie the child)
         */
        final List<PluginParameter> interfaceParameters = PluginParametersFactory.build()
                .addParameter(AParameterPluginImplementation.LONG_PARAM, PluginInterfaceUtilsTest.LONG_STR_VALUE)
                .getParameters();
        final PluginConfiguration pluginConfigurationInterface = PluginUtils
                .getPluginConfiguration(interfaceParameters, AParameterPluginImplementation.class,
                                        Arrays.asList(PLUGIN_CURRENT_PACKAGE));
        Assert.assertNotNull(pluginConfigurationInterface);

        /*
         * Get the configuration for the complex Plugin (ie the parent)
         */
        final List<PluginParameter> complexParameters = PluginParametersFactory.build()
                .addParameterPluginConfiguration(ComplexPlugin.PLUGIN_PARAM, pluginConfigurationInterface)
                .addParameter(ComplexPlugin.ACTIVE, TRUE)
                .addParameter(ComplexPlugin.COEFF, PluginInterfaceUtilsTest.CINQ).getParameters();

        HashMap<Long, Object> instantiatedPluginMap = new HashMap<>();
        instantiatedPluginMap.put(pluginConfigurationInterface.getId(), PluginUtils
                .getPlugin(pluginConfigurationInterface, pluginConfigurationInterface.getPluginClassName(),
                           Arrays.asList(PLUGIN_CURRENT_PACKAGE), instantiatedPluginMap));
        /*
         * Instantiate the parent plugin
         */
        complexPlugin = PluginUtils
                .getPlugin(complexParameters, ComplexPlugin.class, Arrays.asList(PLUGIN_CURRENT_PACKAGE),
                           instantiatedPluginMap);
        Assert.assertNotNull(complexPlugin);

        Assert.assertTrue(complexPlugin.add(Integer.parseInt(PluginInterfaceUtilsTest.CINQ),
                                            Integer.parseInt(PluginInterfaceUtilsTest.QUATRE)) > 0);
        Assert.assertTrue(complexPlugin.echo(PluginInterfaceUtilsTest.HELLO_WORLD)
                                  .contains(PluginInterfaceUtilsTest.HELLO_WORLD));

        LOGGER.info("plugin parameter:" + complexPlugin.echoPluginParameter());

        LOGGER.debug(ENDING + toString());
    }

    /**
     * @ throw if an error occurs
     */
    @Test(expected = PluginUtilsRuntimeException.class)
    @Requirement("REGARDS_DSL_SYS_PLG_020")
    @Purpose("Error to load a plugin from with an incompatible interface parameter.")
    public void incompatibleInterfaceError() {
        LOGGER.debug(STARTING + toString());
        /*
         * Set all parameters
         */

        final List<PluginParameter> complexParameters = PluginParametersFactory.build()
                .addParameter(ComplexErrorPlugin.ACTIVE, TRUE)
                .addParameter(ComplexErrorPlugin.COEFF, PluginInterfaceUtilsTest.CINQ)
                .addParameter(ComplexErrorPlugin.PLUGIN_PARAM, "coucou").getParameters();

        // instantiate plugin
        PluginUtils.getPlugin(complexParameters, ComplexErrorPlugin.class, Arrays.asList(PLUGIN_CURRENT_PACKAGE),
                              new HashMap<>());
    }

    /**
     * @ throw if an error occurs
     */
    @Test(expected = PluginUtilsRuntimeException.class)
    @Requirement("REGARDS_DSL_SYS_PLG_020")
    @Purpose("Error to load a plugin from with an incompatible interface parameter.")
    public void incompatibleParameterError() {
        LOGGER.debug(STARTING + toString());
        /*
         * Set all parameters
         */

        final List<PluginParameter> complexParameters = PluginParametersFactory.build()
                .addParameter(ComplexErrorPlugin.ACTIVE, TRUE).addParameter(ComplexErrorPlugin.COEFF, "allo")
                .addParameter(ComplexErrorPlugin.PLUGIN_PARAM, "lorem ipsum").getParameters();

        // instantiate plugin
        PluginUtils.getPlugin(complexParameters, ComplexErrorPlugin.class, Arrays.asList(PLUGIN_CURRENT_PACKAGE),
                              new HashMap<>());
    }

}
