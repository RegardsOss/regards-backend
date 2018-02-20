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

import com.google.common.base.Optional;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.modules.acquisition.plugins.ISIPGenerationPluginWithMetadataToolbox;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.AbstractProductMetadataPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.Jason2Ptlm1ProductMetadataPlugin;

/**
 *
 * Test des plugins PLTM1 JASON2
 *
 * @author Christophe Mertz
 */
@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@EnableAutoConfiguration
public class Jason2Pltm1PluginTest extends AbstractProductMetadataPluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Jason2PluginTest.class);

    @Autowired
    IPluginService pluginService;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResoler;

    @Override
    public String getProjectProperties() {
        return "ssalto/domain/plugins/impl/jason2plugin.properties";
    }

    @Before
    public void start() {
        runtimeTenantResoler.forceTenant(DEFAULT_TENANT);
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_JASON2_PLTM1", "JASON2/PLTM1",
                         "JA2_PLTM1_P_1025_20081204_114522_20081204_084545_20081204_112230");
        addPluginTestDef("DA_TC_JASON2_PLTM1", "JASON2/PLTM1", "JA2_PLTM1_P_1280_20081226_101827__");
    }

    @Override
    public void initTestSoloList() {

    }

    @Override
    public void createMetadataPlugin_all() {
        super.createMetadataPlugin_all();
    }

    @Override
    public ISIPGenerationPluginWithMetadataToolbox buildPlugin(String datasetName) throws ModuleException {
        PluginConfiguration pluginConfiguration = this
                .getPluginConfiguration("Jason2Ptlm1ProductMetadataPlugin", Optional.of(PluginParametersFactory.build()
                        .addParameter(AbstractProductMetadataPlugin.DATASET_SIP_ID, datasetName)
                        .addParameter(Jason2Ptlm1ProductMetadataPlugin.ORF_FILE_PATH_PARAM,
                                      "src/test/resources/income/data/JASON2/ORF_HISTORIQUE/JA2_ORF_AXXCNE*")
                        .addParameter(Jason2Ptlm1ProductMetadataPlugin.CYCLES_FILE_PATH_PARAM,
                                      "src/test/resources/income/data/JASON2/CYCLES/JASON2_CYCLES")
                        .getParameters()));

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
