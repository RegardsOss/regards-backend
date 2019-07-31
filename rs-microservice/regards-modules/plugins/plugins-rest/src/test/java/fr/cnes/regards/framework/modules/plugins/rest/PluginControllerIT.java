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
package fr.cnes.regards.framework.modules.plugins.rest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.jayway.jsonpath.JsonPath;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.CannotInstanciatePluginException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;

/**
 * Test plugin controller
 * @author Marc Sordi
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=plugin_it",
        "regards.cipher.key-location=src/test/resources/testKey", "regards.cipher.iv=1234567812345678" })
public class PluginControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuntimeTenantResolver resolver;

    @Autowired
    private IPluginConfigurationRepository repository;

    @After
    public void cleanUp() {
        resolver.forceTenant(getDefaultTenant());
        repository.deleteAll();
    }

    @Test
    public void savePluginConfigurationTest() throws ModuleException, NotAvailablePluginConfigurationException {

        // Bad version plugin creation attempt
        // Creation Inner plugin : must fail
        performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS, readJsonContract("innerConfUpdatedVersion.json"),
                           customizer().expect(MockMvcResultMatchers.status().isUnprocessableEntity()),
                           "Configuration should be saved!", "InnerParamTestPlugin");

        // Inner plugin creation
        ResultActions result = performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS,
                                                  readJsonContract("innerConf.json"),
                                                  customizer().expectStatusCreated(), "Configuration should be saved!",
                                                  "InnerParamTestPlugin");
        String resultAsString = payload(result);
        Integer innerConfigId = JsonPath.read(resultAsString, "$.content.id");

        // Creation plugin with inner plugin as parameter
        String json = readJsonContract("fakeConf.json").replace("\"id\":-1", "\"id\": " + innerConfigId.toString());
        result = performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS, json, customizer().expectStatusCreated(),
                                    "Configuration should be saved!", "ParamTestPlugin");
        resultAsString = payload(result);
        // Instanciate plugin
        resolver.forceTenant(getDefaultTenant());
        IParamTestPlugin plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class);
        Assert.assertNotNull(plugin);

        // With dynamic parameter
        String dynValue = "toto";
        plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class,
                                                    IPluginParam.build("pString", dynValue).dynamic());
        Assert.assertNotNull(plugin);

        if (plugin instanceof ParamTestPlugin) {
            ParamTestPlugin p = (ParamTestPlugin) plugin;
            Assert.assertEquals(p.getpString(), dynValue);
        } else {
            Assert.fail();
        }

        // With bad dynamic parameter
        boolean unexpectedValue = false;
        try {
            plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class,
                                                        IPluginParam.build("pString", dynValue).dynamic());
        } catch (PluginUtilsRuntimeException e) {
            unexpectedValue = true;
        }
        Assert.assertTrue(unexpectedValue);

        // With integer dynamic parameter
        Integer dynInt = 10;
        plugin = pluginService.getFirstPluginByType(IParamTestPlugin.class,
                                                    IPluginParam.build("pString", dynValue).dynamic(),
                                                    IPluginParam.build("pInteger", dynInt).dynamic());
        Assert.assertNotNull(plugin);
        if (plugin instanceof ParamTestPlugin) {
            ParamTestPlugin p = (ParamTestPlugin) plugin;
            Assert.assertEquals(p.getpString(), dynValue);
            Assert.assertEquals(p.getpInteger(), dynInt);
        } else {
            Assert.fail();
        }

        // Update Inner Plugin
        json = readJsonContract("innerConfUpdated.json").replace("\"id\":0", "\"id\":" + innerConfigId.toString());
        performDefaultPut(PluginController.PLUGINS_PLUGINID_CONFIGID, json, customizer().expectStatusOk(),
                          "Configuration should be saved!", "InnerParamTestPlugin", innerConfigId);

        // Re-instanciate plugin
        resolver.forceTenant(getDefaultTenant());
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
        performDefaultDelete(PluginController.PLUGINS_PLUGINID_CONFIGID,
                             customizer().expect(MockMvcResultMatchers.status().isForbidden()),
                             "Configuration mustn't have been deleted", "InnerParamTestPlugin", innerConfigId);

        // Update Inner Plugin with a different version (2.0.0)
        json = readJsonContract("innerConfUpdatedVersion.json").replace("\"id\":0",
                                                                        "\"id\":" + innerConfigId.toString());
        performDefaultPut(PluginController.PLUGINS_PLUGINID_CONFIGID, json,
                          customizer().expect(MockMvcResultMatchers.status().isUnprocessableEntity()),
                          "Configuration should be saved!", "InnerParamTestPlugin", innerConfigId);
    }

    @Test(expected = CannotInstanciatePluginException.class)
    public void instantiatePluginConfigurationTest() throws ModuleException, NotAvailablePluginConfigurationException {
        // Inner plugin creation with version 1.0.0
        ResultActions result = performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS,
                                                  readJsonContract("innerConf.json"),
                                                  customizer().expectStatusCreated(), "Configuration should be saved!",
                                                  "InnerParamTestPlugin");
        String resultAsString = payload(result);
        long innerConfigId = (Integer) JsonPath.read(resultAsString, "$.content.id");
        // Remove from cache
        resolver.forceTenant(getDefaultTenant());
        pluginService.cleanPluginCache(innerConfigId);

        // Retrieve PLugin Configuration
        PluginConfiguration pluginConf = pluginService.loadPluginConfiguration(innerConfigId);

        // Change version
        pluginConf.setVersion("3.0.0");
        // Save like a piglet
        repository.save(pluginConf);

        // Try load it
        @SuppressWarnings("unused")
        InnerParamTestPlugin plugin = pluginService.getPlugin(pluginConf.getId());
    }

    @Test
    public void saveConfWithInvalidParameters() {
        // Inner plugin creation
        ResultActions result = performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS,
                                                  readJsonContract("innerConf.json"),
                                                  customizer().expectStatusCreated(), "Configuration should be saved!",
                                                  "InnerParamTestPlugin");
        String resultAsString = payload(result);
        Integer innerConfigId = JsonPath.read(resultAsString, "$.content.id");

        // Creation plugin with inner plugin as parameter
        String json = readJsonContract("fakeConfInvalid.json").replace("\"id\":-1",
                                                                       "\"id\": " + innerConfigId.toString());
        // Errors should be on each numerical value: pByte, pShort, pInteger, pLong
        // Error case on double and float is not tested because large float or double are interpreted as Infinity unless gson breaks.
        performDefaultPost(PluginController.PLUGINS_PLUGINID_CONFIGS, json,
                           customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY).expectIsArray("$.messages")
                                   .expectToHaveSize("$.messages", 4),
                           "Configuration should not be saved!", "ParamTestPlugin");
    }
}
