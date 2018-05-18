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
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.Hy2ADoris10ProductMetadataPlugin;

/**
 * @author Christophe Mertz
 */
public class Hy2APluginTest extends AbstractProductMetadataPluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hy2APluginTest.class);

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
                .getPluginConfiguration("Hy2AProductMetadataPlugin", Optional.of(PluginParametersFactory.build()
                        .addParameter(AbstractProductMetadataPlugin.DATASET_SIP_ID, datasetName)
                        .addParameter(Hy2ADoris10ProductMetadataPlugin.ORF_FILE_PATH_PARAM,
                                      "src/test/resources/income/data/HY2A/ORF_HISTORIQUE/H2A_ORF_AXXCNE*")
                        .getParameters()));

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_HY2A_HISTO_COM", "HY2A/COM");
        addPluginTestDef("DA_TC_HY2A_HISTO_FOUS_BORD", "HY2A/FOUS");
        addPluginTestDef("DA_TC_HY2A_HISTO_MANOEUVRES", "HY2A/MANOEUVRES");
        addPluginTestDef("DA_TC_HY2A_HISTO_ORF", "HY2A/ORF_HISTORIQUE");
        addPluginTestDef("DA_TC_HY2A_LOGVOL_DORIS_DGXX", "HY2A/LOGVOL",
                         "LOGICIEL_DORIS_HY2_400_306_20080310_123000_MEM.REF");
        addPluginTestDef("DA_TC_HY2A_LOGVOL_DORIS_DGXX", "HY2A/LOGVOL",
                         "LOGICIEL_DORIS_HY2_400_306_20080310_123000_RAM.REF");
        addPluginTestDef("DA_TC_HY2A_MESURES_LASER", "HY2A/LASER");
        addPluginTestDef("DA_TC_HY2A_MOE_EXTRA_15J", "HY2A/MOE_EXTRA_15J");
        addPluginTestDef("DA_TC_HY2A_POE", "HY2A/POE");
        addPluginTestDef("DA_TC_HY2A_MOE", "HY2A/MOE");
        addPluginTestDef("DA_TC_HY2A_FREQAJ_BALPUB", "COMMUN/BALISES_PUBLIQUES/FA",
                         "H2A_FBP_AXXCNE20080919_075700_20080920_044831_20080923_235921");
        addPluginTestDef("DA_TC_HY2A_DORIS1B_POE_CDDIS_COM", "HY2A/DORIS1B_POE_CDDIS_COM");
        addPluginTestDef("DA_TC_HY2A_MOE_EXTRA_4J", "HY2A/MOE_EXTRA");
        addPluginTestDef("DA_TC_HY2A_PLTM", "HY2A/PLTM");

        // addPluginTestDef("DA_TC_HY2A_POE_SP3", "HY2A/poe_sp3");
    }

    @Override
    public void initTestSoloList() {
    }

    @Override
    public String getProjectProperties() {
        return "ssalto/domain/plugins/impl/hy2aplugin.properties";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
