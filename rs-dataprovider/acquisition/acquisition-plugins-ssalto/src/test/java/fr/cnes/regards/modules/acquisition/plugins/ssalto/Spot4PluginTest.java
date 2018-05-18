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
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.Spot4ProductMetadataPlugin;

/**
 * Test des plugins SPOT4
 *
 * @author Christophe Mertz
 */
@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@EnableAutoConfiguration
public class Spot4PluginTest extends AbstractProductMetadataPluginTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Jason2OgdrPluginTest.class);

    @Autowired
    IPluginService pluginService;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResoler;

    @Before
    public void start() {
        runtimeTenantResoler.forceTenant(DEFAULT_TENANT);
    }

    @Override
    public ISIPGenerationPluginWithMetadataToolbox buildPlugin(String datasetName) throws ModuleException {
        PluginConfiguration pluginConfiguration = this
                .getPluginConfiguration("Spot4ProductMetadataPlugin",
                                        Optional.of(PluginParametersFactory.build()
                                                .addParameter(AbstractProductMetadataPlugin.DATASET_SIP_ID, datasetName)
                                                .addParameter(Spot4ProductMetadataPlugin.ARC_FILE_PATH_PARAM,
                                                              "src/test/resources/income/data/spot4/arcs/SPOT4_ARCS")
                                                .getParameters()));

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_SPOT4_ARCS_POE", "spot4/arcs");
        addPluginTestDef("DA_TC_SPOT4_HISTO_MANOEUVRES", "spot4/manoeuvres");
        addPluginTestDef("DA_TC_SPOT4_HISTO_COM", "spot4/com");
        addPluginTestDef("DA_TC_SPOT4_DORIS10_COM", "spot4/commerciales_10");
        addPluginTestDef("DA_TC_SPOT4_DORIS10_PUB", "spot4/publiques_10");
        // addPluginTestDef("DA_TC_SPOT4_DORIS1B_POE_CDDIS", "spot4/doris1b_poe_cddis");
        addPluginTestDef("DA_TC_SPOT4_MOE", "spot4/moe");
        addPluginTestDef("DA_TC_SPOT4_POE", "spot4/poe");
        addPluginTestDef("DA_TC_SPOT4_PLTM", "spot4/tmsci_nomi");
        addPluginTestDef("DA_TC_SPOT4_PLTM", "spot4/tmsci_redo");
        addPluginTestDef("DA_TC_SPOT4_HKTM", "spot4/tmsur");
        addPluginTestDef("DA_TC_SPOT4_FREQAJ_BALPUB", "COMMUN/BALISES_PUBLIQUES/FA",
                         "SP4_FBP_AXXCNE20090620_134800_20090619_020807_20090620_015847");
        addPluginTestDef("DA_TC_SPOT4_LOGVOL_DORIS_1G", "spot4/logvol");
        // no data available
        // addPluginTestDef("DA_TC_SPOT4_POE_SP3", "spot4/logvol");
    }

    @Override
    public void initTestSoloList() {
    }

    @Override
    public String getProjectProperties() {
        return "ssalto/domain/plugins/impl/spot4plugin.properties";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
