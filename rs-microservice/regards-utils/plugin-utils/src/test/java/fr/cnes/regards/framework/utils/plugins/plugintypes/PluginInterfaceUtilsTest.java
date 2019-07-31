/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.plugins.plugintypes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.basic.PluginUtilsTestConstants;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;

/**
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
    private static final String PLUGIN_CURRENT_PACKAGE = "fr.cnes.regards.framework.utils.plugins.plugintypes";

    /**
     * A {@link List} of package
     */
    private static final List<String> PLUGIN_PACKAGES = Arrays.asList(PLUGIN_CURRENT_PACKAGE,
                                                                      "fr.cnes.regards.framework.utils.plugins.bean",
                                                                      "fr.cnes.regards.framework.utils.plugins");

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
        PluginUtils.setup(PLUGIN_PACKAGES);
        Set<String> pluginInterfaces = PluginUtils.getPluginInterfaces();
        Assert.assertNotNull(pluginInterfaces);
        pluginInterfaces.forEach(LOGGER::info);
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
        PluginUtils.setup(PLUGIN_EMPTY_PACKAGE);
        Set<String> pluginInterfaces = PluginUtils.getPluginInterfaces();
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
        PluginUtils.setup(PLUGIN_CURRENT_PACKAGE);
        Set<String> pluginInterfaces = PluginUtils.getPluginInterfaces();
        Assert.assertNotNull(pluginInterfaces);
        pluginInterfaces.forEach(LOGGER::info);
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
        PluginUtils.setup(PLUGIN_EMPTY_PACKAGES);
        Set<String> pluginInterfaces = PluginUtils.getPluginInterfaces();
        Assert.assertNotNull(pluginInterfaces);
        Assert.assertTrue(pluginInterfaces.isEmpty());
        LOGGER.debug(ENDING + toString());
    }

    /**
     * Get a {@link ComplexPlugin} with a specific parameters
     * @throws NotAvailablePluginConfigurationException
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_PLG_020")
    @Purpose("Load a plugin with a plugin type interface parameter.")
    public void getComplexPlugin() throws NotAvailablePluginConfigurationException {
        final ComplexPlugin complexPlugin;
        LOGGER.debug(STARTING + toString());
        PluginUtils.setup(PLUGIN_CURRENT_PACKAGE);

        Set<IPluginParam> interfaceParameters = IPluginParam.set(IPluginParam
                .build(AParameterPluginImplementation.FIELD_NAME, PluginInterfaceUtilsTest.LONG_STR_VALUE));
        PluginConfiguration pluginConfigurationInterface = PluginUtils
                .getPluginConfiguration(interfaceParameters, AParameterPluginImplementation.class);
        Assert.assertNotNull(pluginConfigurationInterface);
        pluginConfigurationInterface.setId(100L);

        /*
         * Get the configuration for the complex Plugin (ie the parent)
         */
        final Set<IPluginParam> complexParameters = IPluginParam
                .set(IPluginParam.plugin(ComplexPlugin.FIELD_NAME_PLUGIN, pluginConfigurationInterface.getLabel()),
                     IPluginParam.build(ComplexPlugin.FIELD_NAME_ACTIVE, TRUE),
                     IPluginParam.build(ComplexPlugin.FIELD_NAME_COEF, PluginInterfaceUtilsTest.CINQ));

        HashMap<Long, Object> instantiatedPluginMap = new HashMap<>();
        instantiatedPluginMap.put(pluginConfigurationInterface.getId(),
                                  PluginUtils.getPlugin(pluginConfigurationInterface,
                                                        pluginConfigurationInterface.getPluginClassName(),
                                                        instantiatedPluginMap));
        /*
         * Instantiate the parent plugin
         */
        complexPlugin = PluginUtils.getPlugin(complexParameters, ComplexPlugin.class, instantiatedPluginMap);
        Assert.assertNotNull(complexPlugin);

        Assert.assertTrue(complexPlugin.add(PluginInterfaceUtilsTest.CINQ, PluginInterfaceUtilsTest.QUATRE) > 0);
        Assert.assertTrue(complexPlugin.echo(PluginInterfaceUtilsTest.HELLO_WORLD)
                .contains(PluginInterfaceUtilsTest.HELLO_WORLD));

        LOGGER.info("plugin parameter:" + complexPlugin.echoPluginParameter());

        LOGGER.debug(ENDING + toString());
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    @Requirement("REGARDS_DSL_SYS_PLG_020")
    @Purpose("Error to load a plugin from with an incompatible interface parameter.")
    public void incompatibleInterfaceError() throws NotAvailablePluginConfigurationException {
        LOGGER.debug(STARTING + toString());
        Set<IPluginParam> complexParameters = IPluginParam
                .set(IPluginParam.build(ComplexErrorPlugin.FIELD_NAME_COEF, PluginInterfaceUtilsTest.CINQ),
                     IPluginParam.build(ComplexErrorPlugin.FIELD_NAME_PLUGIN, "coucou"));

        // instantiate plugin
        PluginUtils.setup(PLUGIN_CURRENT_PACKAGE);
        PluginUtils.getPlugin(complexParameters, ComplexErrorPlugin.class, new HashMap<>());
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    @Requirement("REGARDS_DSL_SYS_PLG_020")
    @Purpose("Error to load a plugin from with an incompatible interface parameter.")
    public void incompatibleParameterError() throws NotAvailablePluginConfigurationException {
        LOGGER.debug(STARTING + toString());
        Set<IPluginParam> complexParameters = IPluginParam
                .set(IPluginParam.build(ComplexErrorPlugin.FIELD_NAME_COEF, "allo"),
                     IPluginParam.build(ComplexErrorPlugin.FIELD_NAME_PLUGIN, "lorem ipsum"));

        // instantiate plugin
        PluginUtils.setup(PLUGIN_CURRENT_PACKAGE);
        PluginUtils.getPlugin(complexParameters, ComplexErrorPlugin.class, new HashMap<>());
    }

}
