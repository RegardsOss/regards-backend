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
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.modules.acquisition.plugins.ISIPGenerationPluginWithMetadataToolbox;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.EnvisatPLTMProductMetadataPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.Jason3Gpsp10FlotProductMetadataPlugin;

@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@EnableAutoConfiguration
public class EnvisatPLTMPluginTest extends AbstractProductMetadataPluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvisatPLTMPluginTest.class);

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
        PluginConfiguration pluginConfiguration = this.getPluginConfiguration("EnvisatPLTMProductMetadataPlugin",
                                                                              PluginParametersFactory.build()
                                                                              .addParameter(EnvisatPLTMProductMetadataPlugin.CYCLES_FILE_PATH_PARAM,
                                                                                            "cycles/ENVISAT/ENVISAT_CYCLES")
                                                                              .addParameter(EnvisatPLTMProductMetadataPlugin.ORF_FILE_PATH_PARAM,
                                                                                      "src/test/resources/income/data/ENVISAT/orf/EN1_ORF_AXXCNE*")
                                                                              .getParameters());

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_ENVISAT_PLTM", "ENVISAT/tmsci");
    }

    @Override
    public void initTestSoloList() {
        addPluginTestDef("DA_TC_ENVISAT_PLTM", "ENVISAT/tmsci",
                         "DOR_DOP_0PNPDE20090511_221336_000042722079_00001_37630_8333.N1");
    }

    @Override
    public String getProjectProperties() {
        return "ssalto/domain/plugins/impl/envisatplugin.properties";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
