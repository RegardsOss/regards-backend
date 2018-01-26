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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.plugins.ISIPGenerationPluginWithMetadataToolbox;

/**
 * @author Christophe Mertz
 */
@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@EnableAutoConfiguration
public class Jason2Gpsp10FlotPluginTest extends AbstractProductMetadataPluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Jason2Gpsp10FlotPluginTest.class);

    @Autowired
    IPluginService pluginService;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResoler;

    @Before
    public void start() {
        runtimeTenantResoler.forceTenant(DEFAULT_TENANT);
    }

    @Override
    public ISIPGenerationPluginWithMetadataToolbox buildPlugin() throws ModuleException {
        PluginConfiguration pluginConfiguration = this.getPluginConfiguration("Jason2Gpsp10FlotProductMetadataPlugin");

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_JASON2_GPSP10_FLOT", "JASON2/GPSP10_FLOT");
    }

    @Override
    public void initTestSoloList() {
        addPluginTestDef("DA_TC_JASON2_GPSP10_FLOT", "JASON2/GPSP10_FLOT", "JA2_GPSP1_2008_10_24_21_31_37");
    }

    @Override
    public String getProjectProperties() {
        return "ssalto/domain/plugins/impl/jason2plugin.properties";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
