/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.plugins.generics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.utils.cycle.generics.invalid1.PluginWithCyclicPojoCollection;
import fr.cnes.regards.framework.utils.cycle.generics.invalid2.PluginWithCyclicPojoMap;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;

/**
 * Test plugin containing collection as parameter
 * @author Marc Sordi
 */
public class PluginWithGenericsTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginWithGenericsTest.class);

    @Before
    public void initContext() {
        PluginUtils.setup(this.getClass().getPackage().getName());
    }

    @Test
    public void primitiveTest() throws NotAvailablePluginConfigurationException {

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(PluginWithBoolean.FIELD_NAME_OBJECT, true),
                     IPluginParam.build(PluginWithBoolean.FIELD_NAME_PRIMITIVE, false),
                     IPluginParam.build(PluginWithBoolean.FIELD_NAME_STRING, "string"));

        IPluginWithGenerics plugin = PluginUtils
                .getPlugin(PluginConfiguration.build(PluginWithBoolean.class, "", parameters), null);
        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test
    public void stringCollectionTest() throws NotAvailablePluginConfigurationException {
        List<String> infos = Arrays.asList("info 1", "info 2", "info 3");

        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(PluginWithStringCollection.FIELD_NAME,
                                                                           PluginParameterTransformer.toJson(infos)));

        IPluginWithGenerics plugin = PluginUtils
                .getPlugin(PluginConfiguration.build(PluginWithStringCollection.class, "", parameters), null);

        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test
    public void pojoCollectionTest() throws NotAvailablePluginConfigurationException {

        Info info1 = new Info();
        info1.setMessage("info 1");
        Info info2 = new Info();
        info2.setMessage("info 2");
        Info info3 = new Info();
        info3.setMessage("info 3");

        Set<Info> infos = new HashSet<>(Arrays.asList(info1, info2, info3));

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(PluginWithPojoCollection.FIELD_NAME, PluginParameterTransformer.toJson(infos)));

        IPluginWithGenerics plugin = PluginUtils
                .getPlugin(PluginConfiguration.build(PluginWithPojoCollection.class, "", parameters), null);

        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test
    public void stringMapTest() throws NotAvailablePluginConfigurationException {
        Map<String, String> infos = new HashMap<>();
        infos.put("info1", "info 1");
        infos.put("info2", "info 2");
        infos.put("info3", "info 3");

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(PluginWithStringMap.FIELD_NAME, PluginParameterTransformer.toJson(infos)));

        IPluginWithGenerics plugin = PluginUtils
                .getPlugin(PluginConfiguration.build(PluginWithStringMap.class, "", parameters), null);

        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test
    public void pojoMapTest() throws NotAvailablePluginConfigurationException {
        Info info1 = new Info();
        info1.setMessage("info 1");
        Info info2 = new Info();
        info2.setMessage("info 2");
        Info info3 = new Info();
        info3.setMessage("info 3");
        Map<String, Info> infos = new HashMap<>();
        infos.put("info1", info1);
        infos.put("info2", info2);
        infos.put("info3", info3);

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(PluginWithPojoMap.PARAMETER_NAME, PluginParameterTransformer.toJson(infos)));

        IPluginWithGenerics plugin = PluginUtils
                .getPlugin(PluginConfiguration.build(PluginWithPojoMap.class, "", parameters), null);

        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void cyclicCollectionTest() throws NotAvailablePluginConfigurationException {
        PluginUtils.setup(PluginWithCyclicPojoCollection.class.getPackage().getName());
        PluginUtils.getPlugin(PluginConfiguration.build(PluginWithCyclicPojoCollection.class, "", null), null);
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void cyclicMapTest() throws NotAvailablePluginConfigurationException {
        PluginUtils.setup(PluginWithCyclicPojoMap.class.getPackage().getName());
        PluginUtils.getPlugin(PluginConfiguration.build(PluginWithCyclicPojoMap.class, "", null), null);
    }
}
