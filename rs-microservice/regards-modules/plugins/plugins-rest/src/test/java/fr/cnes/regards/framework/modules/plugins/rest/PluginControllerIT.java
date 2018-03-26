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
package fr.cnes.regards.framework.modules.plugins.rest;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.jayway.jsonpath.JsonPath;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;

/**
 * Test plugin controller
 * @author Marc Sordi
 */
@RegardsTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=plugin_it" })
public class PluginControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuntimeTenantResolver resolver;

    @Test
    public void savePluginConfigurationTest() throws ModuleException {

        pluginService.addPluginPackage(this.getClass().getPackage().getName());

        RequestBuilderCustomizer customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        // Creation Inner plugin
        ResultActions result = performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS,
                                                  readJsonContract("innerConf.json"), customizer,
                                                  "Configuration should be saved!", "InnerParamTestPlugin");
        String resultAsString = payload(result);
        Integer innerConfigId = JsonPath.read(resultAsString, "$.content.id");

        // Creation plugin with inner plugin as parameter
        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        String json = readJsonContract("fakeConf.json").replace("\"id\":-1", "\"id\": " + innerConfigId.toString());
        result = performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS, json, customizer,
                                    "Configuration should be saved!", "ParamTestPlugin");
        resultAsString = payload(result);
        Integer configId = JsonPath.read(resultAsString, "$.content.id");
        // Instanciate plugin
        resolver.forceTenant(DEFAULT_TENANT);
        IParamTestPlugin plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class);
        Assert.assertNotNull(plugin);

        // With dynamic parameter
        String dynValue = "toto";
        PluginParametersFactory dynParametersFactory = PluginParametersFactory.build()
                .addDynamicParameter("pString", dynValue);
        plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class, dynParametersFactory.asArray());
        Assert.assertNotNull(plugin);

        if (plugin instanceof ParamTestPlugin) {
            ParamTestPlugin p = (ParamTestPlugin) plugin;
            Assert.assertEquals(p.getpString(), dynValue);
        } else {
            Assert.fail();
        }

        // With bad dynamic parameter
        dynParametersFactory = PluginParametersFactory.build().addDynamicParameter("pString", "fake");
        boolean unexpectedValue = false;
        try {
            plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class, dynParametersFactory.asArray());
        } catch (PluginUtilsRuntimeException e) {
            unexpectedValue = true;
        }
        Assert.assertTrue(unexpectedValue);

        // With integer dynamic parameter
        Integer dynInt = 10;
        dynParametersFactory = PluginParametersFactory.build().addDynamicParameter("pString", dynValue)
                .addDynamicParameter("pInteger", dynInt);
        plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class, dynParametersFactory.asArray());
        Assert.assertNotNull(plugin);
        if (plugin instanceof ParamTestPlugin) {
            ParamTestPlugin p = (ParamTestPlugin) plugin;
            Assert.assertEquals(p.getpString(), dynValue);
            Assert.assertEquals(p.getpInteger(), dynInt);
        } else {
            Assert.fail();
        }

        // Update Inner Plugin
        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isOk());
        json = readJsonContract("innerConfUpdated.json").replace("\"id\":0", "\"id\":" + innerConfigId.toString());
        performDefaultPut(PluginController.PLUGINS_PLUGINID_CONFIGID, json, customizer,
                          "Configuration should be saved!", "InnerParamTestPlugin", innerConfigId);

        // Re-instanciate plugin
        resolver.forceTenant(DEFAULT_TENANT);
        plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class);
        Assert.assertNotNull(plugin);
        if (plugin instanceof ParamTestPlugin) {
            ParamTestPlugin p = (ParamTestPlugin) plugin;
            Assert.assertTrue(p.getInnerPlugin() instanceof InnerParamTestPlugin);
            Assert.assertEquals("Panthere", ((InnerParamTestPlugin) p.getInnerPlugin()).getToto());
        } else {
            Assert.fail();
        }

        // Try to delete inner configuration
        customizer = getNewRequestBuilderCustomizer();
        customizer.addExpectation(MockMvcResultMatchers.status().isForbidden());
        performDefaultDelete(PluginController.PLUGINS_PLUGINID_CONFIGID, customizer,
                             "COnfiguration mustn't have been deleted", "InnerParamTestPlugin", innerConfigId);
    }
}
