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

import com.google.common.base.Optional;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.modules.acquisition.plugins.ISIPGenerationPluginWithMetadataToolbox;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.AbstractProductMetadataPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.Spot2ProductMetadataPlugin;

/**
 * Test des plugins SPOT2
 *
 * @author Christophe Mertz
 */
public class Spot2PluginTest extends AbstractProductMetadataPluginTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Spot2Doris1bPluginTest.class);

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
                .getPluginConfiguration("Spot2ProductMetadataPlugin",
                                        Optional.of(PluginParametersFactory.build()
                                                .addParameter(AbstractProductMetadataPlugin.DATASET_SIP_ID, datasetName)
                                                .addParameter(Spot2ProductMetadataPlugin.ARC_FILE_PATH_PARAM,
                                                              "src/test/resources/income/data/spot2/arcs/SPOT2_ARCS")
                                                .getParameters()));

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_SPOT2_ARCS_POE", "spot2/arcs");
        addPluginTestDef("DA_TC_SPOT2_HISTO_MANOEUVRES", "spot2/manoeuvres");
        addPluginTestDef("DA_TC_SPOT2_HISTO_COM", "spot2/com");
        addPluginTestDef("DA_TC_SPOT2_DORIS10_COM", "spot2/commerciales_10");
        addPluginTestDef("DA_TC_SPOT2_DORIS10_PUB", "spot2/publiques_10");
        // addPluginTestDef("DA_TC_SPOT2_DORIS1B_POE_CDDIS", "spot2/doris1b_poe_cddis");
        addPluginTestDef("DA_TC_SPOT2_MOE", "spot2/moe");
        addPluginTestDef("DA_TC_SPOT2_POE", "spot2/poe");
        addPluginTestDef("DA_TC_SPOT2_PLTM", "spot2/tmsci_nomi");
        addPluginTestDef("DA_TC_SPOT2_PLTM", "spot2/tmsci_redo");
        addPluginTestDef("DA_TC_SPOT2_HKTM", "spot2/tmsur");
        addPluginTestDef("DA_TC_SPOT2_FREQAJ_BALPUB", "COMMUN/BALISES_PUBLIQUES/FA",
                         "SP2_FBP_AXXCNE20090629_131600_20090628_015936_20090629_014536");
        addPluginTestDef("DA_TC_SPOT2_LOGVOL_DORIS_1G", "spot2/logvol");
    }

    @Override
    public void initTestSoloList() {
    }

    @Override
    public String getProjectProperties() {
        return "ssalto/domain/plugins/impl/spot2plugin.properties";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
