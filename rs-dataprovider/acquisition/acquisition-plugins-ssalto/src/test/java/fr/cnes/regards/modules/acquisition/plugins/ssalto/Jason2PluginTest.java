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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;

/**
 * Test des plugins JASON2 de niveau produit
 * 
 * @author Christophe Mertz
 *
 */
@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@ActiveProfiles({"disableDataProviderTask"})
@EnableAutoConfiguration
public class Jason2PluginTest extends AbstractProductMetadataPluginTest {

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

        addPluginTestDef("DA_TC_JASON2_IGDR", "JASON2/IGDR", "JA2_IPN_2PcP016_166_20081214_053324_20081214_062937");

        addPluginTestDef("DA_TC_JASON2_PAR_LV_DORIS_DGXX", "JASON2/LOGVOL_DORIS",
                         "JA2__1_PAR_DORIS_20080112_140531.REF");
        addPluginTestDef("DA_TC_JASON2_FREQAJ_BALPUB", "COMMUN/BALISES_PUBLIQUES/FA",
                         "JA2_FBP_AXXCNE20080703_120400_20080701_235951_20080702_235551");
        addPluginTestDef("DA_TC_JASON2_HISTO_MANOEUVRES", "JASON2/MANOEUVRES",
                         "JA2_MAN_AXXCNE20081218_084645_20080623_013103_20081218_234740");
        addPluginTestDef("DA_TC_JASON2_CORPFALTI_POE", "JASON2/CORPFALTI_POE",
                         "JA2_VPF_AXVCNE20081205_095500_20080711_215527_20080713_002327");
        addPluginTestDef("DA_TC_JASON2_HISTO_ORF", "JASON2/ORF_HISTORIQUE",
                         "JA2_ORF_AXXCNE20081224_122000_20080704_055707_20090101_081701");
        addPluginTestDef("DA_TC_JASON2_HISTO_FOUS_BORD", "JASON2/FOUS",
                         "JA2_OS1_AXXCNE20081224_121700_20080622_145801_20081224_030002");
        addPluginTestDef("DA_TC_JASON2_LPF_10", "JASON2/LPF_10",
                         "PJ2_LPF_1PaS20080626_072918_20080623_152605_20080623_152606");
        addPluginTestDef("DA_TC_JASON2_PTR_10", "JASON2/PTR_10",
                         "PJ2_PTR_1PaS20080626_070347_20080623_152430_20080623_152502");
        addPluginTestDef("DA_TC_JASON2_LEVEL1_COEFF_AMR", "JASON2/COEF_AMR_L1",
                         "AJ2_AL1_AXVJPL20080620_074625_20080620_074625_20301231_235959");
        addPluginTestDef("DA_TC_JASON2_ANTENNA_TEMPERATURE_COEFF_AMR", "JASON2/COEF_AMR_L1B",
                         "AJ2_ANT_AXXJPL20080620_074625_20080620_074625_20301231_235959");
        addPluginTestDef("DA_TC_JASON2_MOE_EXTRA_7J", "JASON2/MOE_EXTRA_7J",
                         "JA2_PRE_AXPCNE20080625_170400_20080622_215527_20080629_002327");
        addPluginTestDef("DA_TC_JASON2_CORPFALTI_MOE", "JASON2/CORPFALTI_MOE",
                         "JA2_PPF_AXPCNE20080713_121400_20080712_215527_20080714_002327");
        addPluginTestDef("DA_TC_JASON2_CARACTERISATION_P3", "JASON2/EXPERT_POS3",
                         "PJ2_CH1_AXVCNE20080620_074625_20080620_074625_20301231_235959");
        addPluginTestDef("DA_TC_JASON2_COR_IONO_GIM", "JASON2/GIM",
                         "JA2_ION_AXPCNE20080626_094214_20080622_000000_20080622_235959");
        addPluginTestDef("DA_TC_JASON2_LTM_P3", "JASON2/LTM",
                         "PJ2_CA1_AXXCNE20080701_100927_20080615_115927_20080629_120000");
        addPluginTestDef("DA_TC_JASON2_HKTM", "JASON2/HKTM",
                         "JA2_HKTMR_P_1024_20081204_151214_20081204_133914_20081204_134055");
        addPluginTestDef("DA_TC_JASON2_MOE", "JASON2/MOE",
                         "JA2_POR_AXPCNE20080625_170200_20080622_215527_20080624_002327");
        addPluginTestDef("DA_TC_JASON2_TC_LOGBOOK_SSALTO", "JASON2/LOG_SSALTO",
                         "R_TCLOG_JA2_SSALTO_2008_12_04_09_44_57_2008_12_04_11_45_09");
        addPluginTestDef("DA_TC_JASON2_TC_FILE_SSALTO", "JASON2/TC_SSALTO",
                         "JA2_TC_EXCP_DOR-1_TCH_DOR1INITBUL001_2008_09_24_13_30_00");
        addPluginTestDef("DA_TC_JASON2_ZQS_DIODE_DGXX", "JASON2/LOGVOL_DORIS", "JA2__1_ZQS_DIODE_20080902_140000.REF");
        addPluginTestDef("DA_TC_JASON2_ZQS_SYS_DGXX", "JASON2/LOGVOL_DORIS", "JA2__2_ZQS_SYSBD_20080205_015400.REF");
        addPluginTestDef("DA_TC_JASON2_ZQS_RESEAU_BORD_DGXX", "JASON2/LOGVOL_DORIS",
                         "JA2__1_ZQS_BALIS_20081105_081010.REF");
        addPluginTestDef("DA_TC_JASON2_MESURES_LASER", "JASON2/LASER", "jason2_20080625.npt");
        addPluginTestDef("DA_TC_JASON2_SELECTED_HKTMR_QUATERNION", "JASON2/QUATERNIONS",
                         "R_JA2_QUATERNION_2008_10_24_22_00_00_2008_10_26_08_00_00");
        addPluginTestDef("DA_TC_JASON2_SELECTED_HKTMR_SOLARPANEL", "JASON2/PANNEAU_SOLAIRE",
                         "R_JA2_SOLARPANEL_2008_10_24_22_00_00_2008_10_26_08_00_00");
        // KO addPluginTestDef("DA_TC_JASON2_DORIS1B_POE_CDDIS", "JASON2/DORIS1B_POE_CDDIS", "ja2data001.dat.Z"); pas de donnees
        addPluginTestDef("DA_TC_JASON2_PLTM2", "JASON2/PLTM2", "JA2_PLTM2_P_CARMEN2MC_20081204_114541.zip");
        addPluginTestDef("DA_TC_JASON2_HKTM_PASSMC", "JASON2/HKTM_PASSMC", "JA2_HKTMR_P_CARMEN2MC_20081204_151215.zip");
        addPluginTestDef("DA_TC_JASON2_TC_FILE_PASSMC", "JASON2/TC_PASSMC", "JA2_TC_T2L2MC_2008_06_23_14_07_05.zip");
        addPluginTestDef("DA_TC_JASON2_TC_LOGBOOK_PASSMC", "JASON2/LOG_PASSMC",
                         "R_TCLOG_JA2_CARMEN2MC_2008_12_04_11_55_34.zip");
        addPluginTestDef("DA_TC_JASON2_GDR", "JASON2/GDR", "JA2_GPN_2PaP184_254_20050914_100022_20490603_082321");
        addPluginTestDef("DA_TC_JASON2_OGDR", "JASON2/OGDR", "JA2_OPN_2PcS015_184_20081205_003016_20081205_020223");

        addPluginTestDef("DA_TC_JASON2_SAT_ATTITUDE", "JASON2/EXPERT_ATTITUDE", "spa_satatt_jason2.txt");

        // KO addPluginTestDef("DA_TC_JASON2_GPS10_JOUR", "JASON2/GPS10_JOUR", "jas22970.08d.Z"); pas de donnees
        addPluginTestDef("DA_TC_JASON2_LOGVOL_DORIS_DGXX", "JASON2/LOGVOL_DORIS",
                         "LOGICIEL_DORIS_JASON2_400_306_20080310_123000_EEP.REF");
        addPluginTestDef("DA_TC_JASON2_POE", "JASON2/POE",
                         "JA2_VOR_AXVCNE20081205_095000_20080711_215527_20080713_002327");
        addPluginTestDef("DA_TC_JASON2_CYCLES", "JASON2/CYCLES", "JASON2_CYCLES");
        addPluginTestDef("DA_TC_JASON2_DORIS1B_POE_CDDIS_COM", "JASON2/DORIS1B_POE_CDDIS_COM", "DORDATA_021201.JA2");
        addPluginTestDef("DA_TC_JASON2_HISTO_COM", "JASON2/COM",
                         "JA2_COM_AXXCNE20090609_083129_20080621_233045_20090609_060905");

        // acquisition with two ssaltoFiles for 1 product
        List<String> fileNameList = new ArrayList<>();
        fileNameList.add("POS3_1_LV_0102_20071209_EEPROM_BIN");
        fileNameList.add("POS3_1_LV_0102_20071209_EEPROM_HDR");
        addPluginTestDef("DA_TC_JASON2_LOGVOL_POS3", "JASON2/LOGVOL_POS3", fileNameList,
                         "POS3_1_LV_0102_20071209_EEPROM");

        addPluginTestDef("DA_TC_JASON2_CORPFALTI_POE", "JASON2/CORPFALTI_POE",
                         "JA2_VPF_AXVCNE20081205_095500_20080711_215527_20080713_002327");
    }

    @Override
    public void initTestSoloList() {
        addPluginTestDef("DA_TC_JASON2_IGDR", "JASON2/IGDR", "JA2_IPN_2PcP016_166_20081214_053324_20081214_062937");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public IGenerateSIPPlugin buildPlugin() throws ModuleException {
        PluginConfiguration pluginConfiguration = this.getPluginConfiguration("Jason2ProductMetadataPlugin");

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

}