/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.utils.cycle.generics.PluginWithCyclicPojoCollection;
import fr.cnes.regards.framework.utils.cycle.generics.PluginWithCyclicPojoMap;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;

/**
 * Test plugin containing collection as parameter
 *
 * @author Marc Sordi
 *
 */
public class PluginWithGenericsTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginWithGenericsTest.class);

    @Test
    public void primitiveTest() {

        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(PluginWithBoolean.FIELD_NAME_OBJECT, true)
                .addParameter(PluginWithBoolean.FIELD_NAME_PRIMITIVE, false)
                .addParameter(PluginWithBoolean.FIELD_NAME_STRING, "string").getParameters();

        PluginUtils.setup(this.getClass().getPackage().getName());
        IPluginWithGenerics plugin = PluginUtils.getPlugin(parameters, PluginWithBoolean.class, null);
        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test
    public void stringCollectionTest() {
        List<String> infos = Arrays.asList("info 1", "info 2", "info 3");

        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(PluginWithStringCollection.FIELD_NAME, infos).getParameters();

        PluginUtils.setup(this.getClass().getPackage().getName());
        IPluginWithGenerics plugin = PluginUtils.getPlugin(parameters, PluginWithStringCollection.class, null);

        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test
    public void pojoCollectionTest() {
        Info info1 = new Info();
        info1.setMessage("info 1");
        Info info2 = new Info();
        info2.setMessage("info 2");
        Info info3 = new Info();
        info3.setMessage("info 3");

        List<Info> infos = Arrays.asList(info1, info2, info3);

        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(PluginWithPojoCollection.FIELD_NAME, infos).getParameters();

        PluginUtils.setup(this.getClass().getPackage().getName());
        IPluginWithGenerics plugin = PluginUtils.getPlugin(parameters, PluginWithPojoCollection.class, null);

        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test
    public void stringMapTest() {
        Map<String, String> infos = new HashMap<>();
        infos.put("info1", "info 1");
        infos.put("info2", "info 2");
        infos.put("info3", "info 3");

        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(PluginWithStringMap.FIELD_NAME, infos).getParameters();

        PluginUtils.setup(this.getClass().getPackage().getName());
        IPluginWithGenerics plugin = PluginUtils.getPlugin(parameters, PluginWithStringMap.class, null);

        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test
    public void pojoMapTest() {
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

        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(PluginWithPojoMap.PARAMETER_NAME, infos).getParameters();

        PluginUtils.setup(this.getClass().getPackage().getName());
        IPluginWithGenerics plugin = PluginUtils.getPlugin(parameters, PluginWithPojoMap.class, null);

        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void cyclicCollectionTest() {
        PluginUtils.setup(this.getClass().getPackage().getName());
        PluginUtils.getPlugin(null, PluginWithCyclicPojoCollection.class, null);
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void cyclicMapTest() {
        PluginUtils.setup(this.getClass().getPackage().getName());
        PluginUtils.getPlugin(null, PluginWithCyclicPojoMap.class, null);
    }
}
