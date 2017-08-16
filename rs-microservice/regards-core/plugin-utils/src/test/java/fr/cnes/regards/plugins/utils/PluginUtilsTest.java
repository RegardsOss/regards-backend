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
package fr.cnes.regards.plugins.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
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
        String stringPojoParamValue = "value_test";

        LOGGER.debug(STARTING + toString());

        List<String> values = new ArrayList<String>();
        values.add("test1");
        values.add("test2");
        Date currentDate = new Date();
        TestPojo pojoParam = new TestPojo();
        pojoParam.setValue(stringPojoParamValue);
        pojoParam.setValues(values);
        JSONObject object = new JSONObject(pojoParam);
        SimpleDateFormat format = new SimpleDateFormat(PluginParameterUtils.DATE_TIME_FORMAT);
        object.put("date", format.format(currentDate));

        /*
         * Set all parameters
         */
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameter(SamplePlugin.POJO, object.toString()).addParameter(SamplePlugin.SUFFIXE, "chris_test_1")
                .getParameters();

        // instantiate plugin
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE),
                                             new HashMap<>());

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertEquals(Integer.parseInt(PluginUtilsTest.TROIS)
                * (Integer.parseInt(PluginUtilsTest.QUATRE) + Integer.parseInt(PluginUtilsTest.CINQ)),
                            samplePlugin.add(Integer.parseInt(PluginUtilsTest.QUATRE),
                                             Integer.parseInt(PluginUtilsTest.CINQ)));
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.HELLO));
        Assert.assertEquals(samplePlugin.getPojo().getValue(), stringPojoParamValue);
        Assert.assertEquals(samplePlugin.getPojo().getValues().size(), values.size());
        Assert.assertEquals(samplePlugin.getPojo().getDate(), currentDate);
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
                                             new HashMap<>(), aDynamicPlgParam);

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
        samplePlugin = PluginUtils.getPlugin(parameters, SamplePlugin.class, Arrays.asList(PLUGIN_PACKAGE),
                                             new HashMap<>());

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
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameterDynamic(SamplePlugin.SUFFIXE, PluginUtilsTest.RED, dynamicValues).getParameters();
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
                .addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.TROIS)
                .addParameterDynamic(SamplePlugin.SUFFIXE, PluginUtilsTest.RED, dynamicValues).getParameters();

        // init a dynamic parameter
        final PluginParameter aDynamicPlgParam = PluginParametersFactory.build()
                .addParameter(SamplePlugin.SUFFIXE, PluginUtilsTest.CINQ).getParameters().stream().findAny().get();

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
                .build().addParameter(SamplePlugin.ACTIVE, "true")
                .addParameter(SamplePlugin.SUFFIXE, PluginUtilsTest.RED).getParameters();

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
                .build().addParameter(SamplePlugin.ACTIVE, "true")
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.CINQ).getParameters();

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
                .build().addParameter(SamplePlugin.ACTIVE, PluginUtilsTest.TRUE)
                .addParameter(SamplePlugin.SUFFIXE, "chris_test_4")
                .addParameter(SamplePlugin.COEFF, PluginUtilsTest.CINQ).getParameters();

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
