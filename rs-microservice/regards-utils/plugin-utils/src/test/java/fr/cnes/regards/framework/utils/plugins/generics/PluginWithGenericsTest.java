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
package fr.cnes.regards.framework.utils.plugins.generics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;

/**
 * Test plugin containing collection as parameter
 *
 * @author Marc Sordi
 *
 */
public class PluginWithGenericsTest {

    private final Gson gson = new Gson();

    @Test
    public void primitiveTest() {

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(PluginWithBoolean.FIELD_NAME_OBJECT, "true")
                .addParameter(PluginWithBoolean.FIELD_NAME_PRIMITIVE, "false").getParameters();

        IPluginWithGenerics plugin = PluginUtils.getPlugin(parameters, PluginWithBoolean.class,
                                                           Arrays.asList(this.getClass().getPackage().getName()), null);
        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test
    public void stringCollectionTest() {
        List<String> infos = Arrays.asList("info 1", "info 2", "info 3");

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(PluginWithStringCollection.FIELD_NAME, gson.toJson(infos)).getParameters();

        IPluginWithGenerics plugin = PluginUtils.getPlugin(parameters, PluginWithStringCollection.class,
                                                           Arrays.asList(this.getClass().getPackage().getName()), null);

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

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(PluginWithPojoCollection.FIELD_NAME, gson.toJson(infos)).getParameters();

        IPluginWithGenerics plugin = PluginUtils.getPlugin(parameters, PluginWithPojoCollection.class,
                                                           Arrays.asList(this.getClass().getPackage().getName()), null);

        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test
    public void stringMapTest() {
        Map<String, String> infos = new HashMap<>();
        infos.put("info1", "info 1");
        infos.put("info2", "info 2");
        infos.put("info3", "info 3");

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(PluginWithStringMap.FIELD_NAME, gson.toJson(infos)).getParameters();

        IPluginWithGenerics plugin = PluginUtils.getPlugin(parameters, PluginWithStringMap.class,
                                                           Arrays.asList(this.getClass().getPackage().getName()), null);

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

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(PluginWithPojoMap.PARAMETER_NAME, gson.toJson(infos)).getParameters();

        IPluginWithGenerics plugin = PluginUtils.getPlugin(parameters, PluginWithPojoMap.class,
                                                           Arrays.asList(this.getClass().getPackage().getName()), null);

        Assert.assertNotNull(plugin);
        plugin.doIt();
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void cyclicCollectionTest() {
        PluginUtils.getPlugin(null, PluginWithCyclicPojoCollection.class,
                              Arrays.asList(this.getClass().getPackage().getName()), null);
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void cyclicMapTest() {
        PluginUtils.getPlugin(null, PluginWithCyclicPojoMap.class,
                              Arrays.asList(this.getClass().getPackage().getName()), null);
    }
}
