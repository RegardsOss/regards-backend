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
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.SaralProductMetadataPlugin;

/**
 * @author Christophe Mertz
 */
@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@EnableAutoConfiguration
public class SaralPluginTest extends AbstractProductMetadataPluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaralProductMetadataPlugin.class);

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
        PluginConfiguration pluginConfiguration = this
                .getPluginConfiguration("SaralProductMetadataPlugin", Optional.of(PluginParametersFactory.build()
                        .addParameter(SaralProductMetadataPlugin.ORF_FILE_PATH_PARAM,
                                      "src/test/resources/income/data/SARAL/ORF_HISTORIQUE/SRL_ORF_AXXCNE*")
                        .addParameter(SaralProductMetadataPlugin.CYCLES_FILE_PATH_PARAM,
                                      "src/test/resources/income/data/SARAL/CYCLES/SARAL_CYCLES")
                        .getParameters()));

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_SARAL_HISTO_ORF", "SARAL/ORF_HISTORIQUE");
        addPluginTestDef("DA_TC_SARAL_HISTO_MANOEUVRES", "SARAL/MANOEUVRES");
        addPluginTestDef("DA_TC_SARAL_HISTO_COM", "SARAL/COM");
        addPluginTestDef("DA_TC_SARAL_HISTO_FOUS_BORD", "SARAL/FOUS");
        addPluginTestDef("DA_TC_SARAL_MESURES_LASER", "SARAL/LASER");
        addPluginTestDef("DA_TC_SARAL_CYCLES", "SARAL/CYCLES");
        addPluginTestDef("DA_TC_SARAL_CORPFALTI_POE", "SARAL/CORPFALTI_POE");
        addPluginTestDef("DA_TC_SARAL_CORPFALTI_MOE", "SARAL/CORPFALTI_MOE");
        addPluginTestDef("DA_TC_SARAL_COR_IONO_GIM", "SARAL/GIM");
        addPluginTestDef("DA_TC_SARAL_DORIS1B_POE_CDDIS_COM", "SARAL/DORIS1B_POE_CDDIS_COM");
        addPluginTestDef("DA_TC_SARAL_MOE_EXTRA_4J", "SARAL/MOE_EXTRA");
        addPluginTestDef("DA_TC_SARAL_MOE", "SARAL/MOE");
        addPluginTestDef("DA_TC_SARAL_MOE_EXTRA_15J", "SARAL/MOE_EXTRA_15J");
        addPluginTestDef("DA_TC_SARAL_PAR_LV_DORIS_DGXX", "SARAL/LOGVOL_DORIS", "ALK__2_PAR_DORIS_20081010_140531.REF");
        addPluginTestDef("DA_TC_SARAL_PAR_LV_DORIS_DGXX", "SARAL/LOGVOL_DORIS", "ALK__1_PAR_DORIS_20080712_140531.REF");
        addPluginTestDef("DA_TC_SARAL_POE", "SARAL/POE");
        addPluginTestDef("DA_TC_SARAL_TC_LOGBOOK", "SARAL/LOG_SARAL");
        addPluginTestDef("DA_TC_SARAL_ZQS_DIODE_DGXX", "SARAL/LOGVOL_DORIS", "ALK__2_ZQS_DIODE_20080610_061553.REF");
        addPluginTestDef("DA_TC_SARAL_ZQS_DIODE_DGXX", "SARAL/LOGVOL_DORIS", "ALK__1_ZQS_DIODE_20080902_140000.REF");
        addPluginTestDef("DA_TC_SARAL_ZQS_PAR_SYS_DGXX", "SARAL/LOGVOL_DORIS", "ALK__1_ZQS_SYSBD_20080515_182515.REF");
        addPluginTestDef("DA_TC_SARAL_ZQS_PAR_SYS_DGXX", "SARAL/LOGVOL_DORIS", "ALK__2_ZQS_SYSBD_20080205_015400.REF");
        addPluginTestDef("DA_TC_SARAL_ZQS_RESEAU_BORD_DGXX", "SARAL/LOGVOL_DORIS",
                         "ALK__1_ZQS_BALIS_20081105_081010.REF");
        addPluginTestDef("DA_TC_SARAL_ZQS_RESEAU_BORD_DGXX", "SARAL/LOGVOL_DORIS",
                         "ALK__2_ZQS_BALIS_20080512_175213.REF");
        addPluginTestDef("DA_TC_SARAL_MESURES_LASER", "SARAL/LASER");
        addPluginTestDef("DA_TC_SARAL_FREQAJ_BALPUB", "COMMUN/BALISES_PUBLIQUES/FA",
                         "SRL_FBP_AXXCNE20080919_075700_20080920_044831_20080923_235921");
        addPluginTestDef("DA_TC_SARAL_FREQAJ_BALPUB", "COMMUN/BALISES_PUBLIQUES/FA",
                         "SRL_FBP_AXXCNE20080919_075700_20080920_044831_20080923_235921");

        // no data available
        // addPluginTestDef("DA_TC_SARAL_BDR_ALTIKA", "COMMUN/BALISES_PUBLIQUES/FA");
        // addPluginTestDef("DA_TC_SARAL_LOGVOL_ALTIKA", "SARAL/IGDR");
        // addPluginTestDef("DA_TC_SARAL_LOGVOL_ICU", "SARAL/EXPERT_ALTIKA");
        // addPluginTestDef("DA_TC_SARAL_LOGVOL_ICU_TABLE", "SARAL/EXPERT_ALTIKA");
        // addPluginTestDef("DA_TC_SARAL_MNT_ALTIKA", "SARAL/EXPERT_ALTIKA");
        // addPluginTestDef("DA_TC_SARAL_PATCH_ICU", "SARAL/EXPERT_ALTIKA");
        // addPluginTestDef("DA_TC_SARAL_RGUIER_BALPUB", "SARAL/EXPERT_ALTIKA");
        // addPluginTestDef("DA_TC_SARAL_XNG_7J_ALTIKA", "SARAL/XNG");
        // addPluginTestDef("DA_TC_SARAL_XNG_CYCLE_ALTIKA", "SARAL/XNG");

        addPluginTestDef("DA_TC_SARAL_CARACTERISATION_ALTIKA", "SARAL/EXPERT_ALTIKA",
                         "ALK_CHA_AXVCNE20130621_120000_20100101_000000_20301231_235959");
        addPluginTestDef("DA_TC_SARAL_CARACTERISATION_RAD_ALTIKA", "SARAL/EXPERT_ALTIKA",
                         "ALK_CHR_AXVCNE20110207_180000_20110101_000000_20301231_235959");
        addPluginTestDef("DA_TC_SARAL_COP_MOE", "SARAL/COP_MOE",
                         "SRL_PCP_AXVCNE20130717_124200_20130715_215525_20130717_002325");
        addPluginTestDef("DA_TC_SARAL_COP_POE", "SARAL/COP_MOE",
                         "SRL_VCP_AXVCNE20130702_133700_20130522_215525_20130524_002325");
        addPluginTestDef("DA_TC_SARAL_COR_IONO_GIM_NRT", "SARAL/GIM_NRT");

        addPluginTestDef("DA_TC_SARAL_DORIS10_REDATE", "SARAL/RINEX");
        addPluginTestDef("DA_TC_SARAL_GDR", "SARAL/GDR",
                         "SRL_GPN_2PTP001_0014_20130314_163321_20130314_172339.CNES.nc");
        addPluginTestDef("DA_TC_SARAL_HKTM", "SARAL/DECOM_HKTM");
        addPluginTestDef("DA_TC_SARAL_IGDR", "SARAL/IGDR");
        addPluginTestDef("DA_TC_SARAL_LEVEL2_GDR_CONTEXT", "SARAL/GDR",
                         "SRL_LEVEL2_GDR_CONTEXT_001_20130314_053927_20130321_060938");
        addPluginTestDef("DA_TC_SARAL_LTM_ALK", "SARAL/LTM");
        // addPluginTestDef("DA_TC_SARAL_MOE_SP3_EXTRA_15J", "SARAL/MOE_PRE");
        addPluginTestDef("DA_TC_SARAL_OGDR", "SARAL/OGDR");
        addPluginTestDef("DA_TC_SARAL_PLTM", "SARAL/PLTM");
        // addPluginTestDef("DA_TC_SARAL_POE_SP3", "SARAL/POE_SP3");
        addPluginTestDef("DA_TC_SARAL_TC_FILE", "SARAL/TC");
        addPluginTestDef("DA_TC_SARAL_XNG_3J", "SARAL/XNG");

    }

    @Override
    public void initTestSoloList() {
        addPluginTestDef("DA_TC_SARAL_COR_IONO_GIM", "SARAL/GIM");
    }

    @Override
    public String getProjectProperties() {
        return "ssalto/domain/plugins/impl/saralplugin.properties";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
