/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParamDescriptor;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.PluginParamType;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginParameterUtils.PrimitiveObject;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unit testing of {@link PluginUtils}.
 *
 * @author Christophe Mertz
 */
public class PluginUtilsTest extends PluginUtilsTestConstants {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtilsTest.class);

    @Before
    public void initContext() {
        PluginUtils.setup(SamplePlugin.class.getPackage().getName());
    }

    /**
     * Load all plugins
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_PLG_010")
    @Purpose("A plugin is defined with mate-data like, a name, a description, the author, the version, the licence...")
    public void loadPlugins() {
        LOGGER.debug(STARTING + this);

        // Get all the plugins
        Map<String, PluginMetaData> maps = PluginUtils.getPlugins();
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
        Assert.assertNotNull(pluginMetaData.getLicense());

        LOGGER.debug(ENDING + this);
    }

    @Test
    public void testMarkdownMetadata() {
        PluginUtils.setup(SamplePlugin.class.getPackage().getName());
        PluginMetaData mtd = PluginUtils.createPluginMetaData(SamplePlugin.class);

        Assert.assertNotNull(mtd);
        Assert.assertTrue(mtd.getMarkdown() != null && !mtd.getMarkdown().isEmpty());
        for (PluginParamDescriptor ptype : mtd.getParameters()) {
            if (SamplePlugin.FIELD_NAME_SUFFIX.equals(ptype.getName())) {
                Assert.assertTrue(ptype.getMarkdown() != null && !ptype.getMarkdown().isEmpty());
            }
            if (SamplePlugin.FIELD_NAME_SUFFIX_USER.equals(ptype.getName())) {
                Assert.assertTrue(ptype.getUserMarkdown() != null && !ptype.getUserMarkdown().isEmpty());
            }
        }
    }

    @Test
    public void getSamplePlugin() throws NotAvailablePluginConfigurationException {

        LOGGER.debug(STARTING + this);

        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SamplePlugin.FIELD_NAME_ACTIVE,
                                                                           PluginUtilsTest.TRUE),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_COEF,
                                                                           PluginUtilsTest.TROIS),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX,
                                                                           "chris_test_1"));

        // instantiate plugin
        SamplePlugin samplePlugin = PluginUtils.getPlugin(PluginConfiguration.build(SamplePlugin.class, "", parameters),
                                                          new ConcurrentHashMap<>());

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertEquals(PluginUtilsTest.TROIS * (PluginUtilsTest.QUATRE + PluginUtilsTest.CINQ),
                            samplePlugin.add(PluginUtilsTest.QUATRE, PluginUtilsTest.CINQ));
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.HELLO));
        LOGGER.debug(ENDING + this);
    }

    @Test
    public void getSamplePluginWithOneDynamicParameter() throws NotAvailablePluginConfigurationException {

        LOGGER.debug(STARTING + this);

        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SamplePlugin.FIELD_NAME_ACTIVE,
                                                                           PluginUtilsTest.TRUE),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_COEF,
                                                                           PluginUtilsTest.TROIS).dynamic(),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX, "suffix"));

        IPluginParam dynParam = IPluginParam.build(SamplePlugin.FIELD_NAME_COEF, -1);

        // instantiate plugin
        SamplePlugin samplePlugin = PluginUtils.getPlugin(PluginConfiguration.build(SamplePlugin.class, "", parameters),
                                                          new ConcurrentHashMap<>(),
                                                          dynParam);

        Assert.assertNotNull(samplePlugin);

        // Use plugin
        Assert.assertTrue(0 > samplePlugin.add(PluginUtilsTest.QUATRE, PluginUtilsTest.CINQ));
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.HELLO));
        LOGGER.debug(ENDING + this);
    }

    @Test
    public void getSamplePluginDynamicParameterNull() throws NotAvailablePluginConfigurationException {

        LOGGER.debug(STARTING + this);

        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SamplePlugin.FIELD_NAME_ACTIVE,
                                                                           PluginUtilsTest.TRUE),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_COEF,
                                                                           PluginUtilsTest.TROIS).dynamic(),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX, "a suffix"));

        // instantiate plugin
        SamplePlugin samplePlugin = PluginUtils.getPlugin(PluginConfiguration.build(SamplePlugin.class, "", parameters),
                                                          new ConcurrentHashMap<>());

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(0 < samplePlugin.add(PluginUtilsTest.QUATRE, PluginUtilsTest.CINQ));
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.HELLO));
        LOGGER.debug(ENDING + this);
    }

    @Test
    public void getSamplePluginWithOneDynamicParameterWithValues() throws NotAvailablePluginConfigurationException {

        LOGGER.debug(STARTING + this);

        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SamplePlugin.FIELD_NAME_ACTIVE,
                                                                           PluginUtilsTest.TRUE),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_COEF,
                                                                           PluginUtilsTest.TROIS).dynamic(),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX, "a suffix")
                                                                    .dynamic(PluginUtilsTest.RED,
                                                                             PluginUtilsTest.BLUE,
                                                                             PluginUtilsTest.GREEN));

        // Init a dynamic parameter
        IPluginParam dyn = IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX, PluginUtilsTest.BLUE);

        // instantiate plugin
        SamplePlugin samplePlugin = PluginUtils.getPlugin(PluginConfiguration.build(SamplePlugin.class, "", parameters),
                                                          new ConcurrentHashMap<>(),
                                                          dyn);

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.BLUE));
        LOGGER.debug(ENDING + this);
    }

    @Test
    public void getSamplePluginWithoutDynamicParameterWithValues() throws NotAvailablePluginConfigurationException {

        LOGGER.debug(STARTING + this);

        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SamplePlugin.FIELD_NAME_ACTIVE,
                                                                           PluginUtilsTest.TRUE),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_COEF,
                                                                           PluginUtilsTest.TROIS).dynamic(),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX,
                                                                           PluginUtilsTest.RED)
                                                                    .dynamic(PluginUtilsTest.RED,
                                                                             PluginUtilsTest.BLUE,
                                                                             PluginUtilsTest.GREEN));

        // instantiate plugin
        SamplePlugin samplePlugin = PluginUtils.getPlugin(PluginConfiguration.build(SamplePlugin.class, "", parameters),
                                                          new ConcurrentHashMap<>());

        Assert.assertNotNull(samplePlugin);

        /*
         * Use the plugin
         */
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.RED));
        LOGGER.debug(ENDING + this);
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void getSamplePluginWithUnknownDynamicParameterWithValues() throws NotAvailablePluginConfigurationException {
        LOGGER.debug(STARTING + this);

        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SamplePlugin.FIELD_NAME_ACTIVE,
                                                                           PluginUtilsTest.TRUE),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_COEF,
                                                                           PluginUtilsTest.TROIS).dynamic(),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX,
                                                                           PluginUtilsTest.RED)
                                                                    .dynamic(PluginUtilsTest.RED,
                                                                             PluginUtilsTest.BLUE,
                                                                             PluginUtilsTest.GREEN));

        // Init a dynamic parameter
        IPluginParam dyn = IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX, PluginUtilsTest.CINQ);

        // instantiate plugin
        PluginUtils.getPlugin(PluginConfiguration.build(SamplePlugin.class, "", parameters),
                              new ConcurrentHashMap<>(),
                              dyn);
    }

    @Test
    public void getSamplePluginMissingIntegerParameter() throws NotAvailablePluginConfigurationException {

        LOGGER.debug(STARTING + this);

        // Set parameters : Missing coeff parameter
        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SamplePlugin.FIELD_NAME_ACTIVE,
                                                                           PluginUtilsTest.TRUE),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX,
                                                                           PluginUtilsTest.RED));

        // instantiate plugin
        PluginUtils.setup(SamplePlugin.class.getPackage().getName());
        SamplePlugin samplePlugin = PluginUtils.getPlugin(PluginConfiguration.build(SamplePlugin.class, "", parameters),
                                                          new ConcurrentHashMap<>());

        Assert.assertNotNull(samplePlugin);

        // Use the plugin
        Assert.assertTrue(samplePlugin.echo(PluginUtilsTest.HELLO).contains(PluginUtilsTest.RED));
        Assert.assertTrue(0 > samplePlugin.add(10, 15));
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void getSamplePluginMissingStringParameter() throws NotAvailablePluginConfigurationException {

        LOGGER.debug(STARTING + this);

        // Set parameters : Missing suffix parameter
        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SamplePlugin.FIELD_NAME_ACTIVE,
                                                                           PluginUtilsTest.TRUE),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_COEF,
                                                                           PluginUtilsTest.CINQ));

        PluginUtils.getPlugin(PluginConfiguration.build(SamplePlugin.class, "", parameters), new ConcurrentHashMap<>());

        // Use the plugin
        Assert.fail();
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void getSamplePluginWithErrorInitMethod() throws NotAvailablePluginConfigurationException {
        LOGGER.debug(STARTING + this);

        // Set parameters
        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SamplePlugin.FIELD_NAME_ACTIVE,
                                                                           PluginUtilsTest.TRUE),
                                                        IPluginParam.build(SampleErrorPlugin.FIELD_NAME_SUFFIX,
                                                                           "chris_test_4"),
                                                        IPluginParam.build(SamplePlugin.FIELD_NAME_COEF,
                                                                           PluginUtilsTest.CINQ));

        // instantiate plugin
        PluginUtils.getPlugin(PluginConfiguration.build(SampleErrorPlugin.class, "", parameters),
                              new ConcurrentHashMap<>());
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

    @Test
    public void testPluginMetaDataOnEnumCollection() {
        PluginMetaData mtd = PluginUtils.createPluginMetaData(SamplePlugin.class);
        PluginParamDescriptor pluginParamDesc = mtd.getParameters()
                                                   .stream()
                                                   .filter(param -> param.getName().equals("someEnums"))
                                                   .findFirst()
                                                   .get();
        Assert.assertEquals(PluginParamType.COLLECTION, pluginParamDesc.getType());
        Assert.assertEquals(PluginParamType.STRING, pluginParamDesc.getParameterizedSubTypes()[0]);
    }

}
