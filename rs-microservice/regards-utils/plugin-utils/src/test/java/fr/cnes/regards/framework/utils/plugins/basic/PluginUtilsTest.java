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
package fr.cnes.regards.framework.utils.plugins.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameterType;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginParameterUtils.PrimitiveObject;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;

/**
 * Unit testing of {@link PluginUtils}.
 *
 * @author Christophe Mertz
 */
public class PluginUtilsTest extends PluginUtilsTestConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtilsTest.class);

    private static final String PLUGIN_PACKAGE = "fr.cnes.regards.framework.utils.plugins.basic";

    private final Gson gson = new Gson();

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

    @Test
    public void testMarkdownMetadata() {
        PluginMetaData mtd = PluginUtils.createPluginMetaData(SamplePlugin.class,
                                                              SamplePlugin.class.getPackage().getName());
        Assert.assertNotNull(mtd);
        Assert.assertTrue((mtd.getMarkdown() != null) && !mtd.getMarkdown().isEmpty());
        for (PluginParameterType ptype : mtd.getParameters()) {
            if (SamplePlugin.FIELD_NAME_SUFFIX.equals(ptype.getName())) {
                Assert.assertTrue((ptype.getMarkdown() != null) && !ptype.getMarkdown().isEmpty());
            }
        }
        LOGGER.debug(gson.toJson(mtd));
    }

    /**
     * Get a {@link SamplePlugin} with a specific parameters
     */
    @Test
    public void getSamplePlugin() {
        SamplePlugin samplePlugin = null;

        LOGGER.debug(STARTING + toString());

        List<String> values = new ArrayList<String>();
        values.add("test1");
        values.add("test2");
        /*
         * Set all parameters
         */
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.FIELD_NAME_ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.FIELD_NAME_COEF, PluginUtilsTest.TROIS)
                .addParameter(SamplePlugin.FIELD_NAME_SUFFIX, "chris_test_1").getParameters();

        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE),
                                             new HashMap<>());

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertEquals(PluginUtilsTest.TROIS * (PluginUtilsTest.QUATRE + PluginUtilsTest.CINQ),
                            samplePlugin.add(PluginUtilsTest.QUATRE, PluginUtilsTest.CINQ));
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
                .addParameter(SamplePlugin.FIELD_NAME_ACTIVE, PluginUtilsTest.TRUE)
                .addDynamicParameter(SamplePlugin.FIELD_NAME_COEF, PluginUtilsTest.TROIS)
                .addParameter(SamplePlugin.FIELD_NAME_SUFFIX, "suffix").getParameters();
        // init a dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                .addParameter(SamplePlugin.FIELD_NAME_COEF, -1).getParameters().stream().findAny().get();

        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE),
                                             new HashMap<>(), aDynamicPlgParam);

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(0 > samplePlugin.add(PluginUtilsTest.QUATRE, PluginUtilsTest.CINQ));
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
                .addParameter(SamplePlugin.FIELD_NAME_ACTIVE, PluginUtilsTest.TRUE)
                .addDynamicParameter(SamplePlugin.FIELD_NAME_COEF, PluginUtilsTest.TROIS)
                .addParameter(SamplePlugin.FIELD_NAME_SUFFIX, "a suffix").getParameters();
        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE),
                                             new HashMap<>());

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(0 < samplePlugin.add(PluginUtilsTest.QUATRE, PluginUtilsTest.CINQ));
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
                .addParameter(SamplePlugin.FIELD_NAME_ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.FIELD_NAME_COEF, PluginUtilsTest.TROIS)
                .addDynamicParameter(SamplePlugin.FIELD_NAME_SUFFIX, PluginUtilsTest.RED, dynamicValues)
                .getParameters();
        // init a dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                .addParameter(SamplePlugin.FIELD_NAME_SUFFIX, PluginUtilsTest.BLUE).getParameters().stream().findAny()
                .get();

        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE),
                                             new HashMap<>(), aDynamicPlgParam);

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
                .addParameter(SamplePlugin.FIELD_NAME_ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.FIELD_NAME_COEF, PluginUtilsTest.TROIS)
                .addDynamicParameter(SamplePlugin.FIELD_NAME_SUFFIX, PluginUtilsTest.RED, dynamicValues)
                .getParameters();
        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE),
                                             new HashMap<>());

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
                .addParameter(SamplePlugin.FIELD_NAME_ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.FIELD_NAME_COEF, PluginUtilsTest.TROIS)
                .addDynamicParameter(SamplePlugin.FIELD_NAME_SUFFIX, PluginUtilsTest.RED, dynamicValues)
                .getParameters();

        // init a dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                .addParameter(SamplePlugin.FIELD_NAME_SUFFIX, PluginUtilsTest.CINQ).getParameters().stream().findAny()
                .get();

        // instantiate plugin
        PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE), new HashMap<>(),
                              aDynamicPlgParam);
    }

    /**
     * Used the default parameter value when the Integer parameter is missing
     */
    @Test
    public void getSamplePluginMissingIntegerParameter() {
        SamplePlugin samplePlugin = null;

        LOGGER.debug(STARTING + toString());

        // Set parameters : Missing coeff parameter
        final List<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> parameters = PluginParametersFactory
                .build().addParameter(SamplePlugin.FIELD_NAME_ACTIVE, Boolean.TRUE)
                .addParameter(SamplePlugin.FIELD_NAME_SUFFIX, PluginUtilsTest.RED).getParameters();

        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class,
                                             Arrays.asList(SamplePlugin.class.getPackage().getName()), new HashMap<>());

        Assert.assertNotNull(samplePlugin);

        // Use the plugin
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.RED));
        Assert.assertTrue(0 > samplePlugin.add(10, 15));
    }

    @Test
    public void getSamplePluginMissingStringParameter() {
        SamplePlugin samplePlugin = null;

        LOGGER.debug(STARTING + toString());

        // Set parameters : Missing suffix parameter
        final List<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> parameters = PluginParametersFactory
                .build().addParameter(SamplePlugin.FIELD_NAME_ACTIVE, Boolean.TRUE)
                .addParameter(SamplePlugin.FIELD_NAME_COEF, PluginUtilsTest.CINQ).getParameters();

        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class,
                                             Arrays.asList("fr.cnes.regards.plugins.utils.plugintypes"),
                                             new HashMap<>());

        // Use the plugin
        Assert.assertNotNull(samplePlugin);
    }

    /**
     * Unable to get {@link SamplePlugin} an Integer parameter is missing
     */
    @Test(expected = PluginUtilsRuntimeException.class)
    public void getSamplePluginWithErrorInitMethod() {
        LOGGER.debug(STARTING + toString());

        /*
         * Set parameters
         */
        final List<fr.cnes.regards.framework.modules.plugins.domain.PluginParameter> parameters = PluginParametersFactory
                .build().addParameter(SampleErrorPlugin.FIELD_NAME_ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SampleErrorPlugin.FIELD_NAME_SUFFIX, "chris_test_4")
                .addParameter(SampleErrorPlugin.FIELD_NAME_COEF, PluginUtilsTest.CINQ).getParameters();

        // instantiate plugin
        PluginUtils.getPlugin(parameters, SampleErrorPlugin.class, Arrays.asList(PLUGIN_PACKAGE), new HashMap<>());
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
