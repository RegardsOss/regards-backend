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
import org.springframework.test.context.ContextConfiguration;

import com.google.common.base.Optional;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.modules.acquisition.plugins.ISIPGenerationPluginWithMetadataToolbox;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.Cryosat2Doris10ProductMetadataPlugin;

/**
 * Test des plugins CRYOSAT2 de niveau produit
 *
 * @author Christophe Mertz
 */
@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@EnableAutoConfiguration
public class Cryosat2PluginTest extends AbstractProductMetadataPluginTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Jason2PluginTest.class);

    @Autowired
    IRuntimeTenantResolver runtimeTenantResoler;

    @Before
    public void start() {
        runtimeTenantResoler.forceTenant(DEFAULT_TENANT);
    }

    @Override
    public ISIPGenerationPluginWithMetadataToolbox buildPlugin() throws ModuleException {
        PluginConfiguration pluginConfiguration = this
                .getPluginConfiguration("Cryosat2ProductMetadataPlugin",
                                        Optional.of(PluginParametersFactory.build()
                                                .addParameter(Cryosat2Doris10ProductMetadataPlugin.ORF_FILE_PATH_PARAM,
                                                              "src/test/resources/income/data/cryosat2/orf/CS__ORF_AXXCNE*")
                                                .getParameters()));

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_CRYOSAT2_HISTO_ORF", "cryosat2/orf");
        addPluginTestDef("DA_TC_CRYOSAT2_HISTO_MANOEUVRES", "cryosat2/manoeuvres",
                         "CS__MAN_AXXCNE20090429_125754_20100710_030000_20100712_050500");

        List<String> fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_REP_MACP___20100711T145514_99999999T999999_0001.HDR");
        fileList.add("CS_OPER_REP_MACP___20100711T145514_99999999T999999_0001.DBL");
        addPluginTestDef("DA_TC_CRYOSAT2_MANOEUVRES_ACP", "cryosat2/manoeuvres", fileList,
                         "CS_OPER_REP_MACP___20100711T145514_99999999T999999_0001");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_REP_MCSF___20100710T150251_99999999T999999_0001.HDR");
        fileList.add("CS_OPER_REP_MCSF___20100710T150251_99999999T999999_0001.DBL");
        addPluginTestDef("DA_TC_CRYOSAT2_MANOEUVRES_CSF", "cryosat2/manoeuvres", fileList,
                         "CS_OPER_REP_MCSF___20100710T150251_99999999T999999_0001");

        addPluginTestDef("DA_TC_CRYOSAT2_HISTO_COM", "cryosat2/com");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_REP_SMPR___20090421T114616_99999999T999999_0001.HDR");
        fileList.add("CS_OPER_REP_SMPR___20090421T114616_99999999T999999_0001.DBL");
        addPluginTestDef("DA_TC_CRYOSAT2_COM", "cryosat2/com_brut", fileList,
                         "CS_OPER_REP_SMPR___20090421T114616_99999999T999999_0001");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_AUX_IONGIM_20100710T000000_20100710T235959_0002.HDR");
        fileList.add("CS_OPER_AUX_IONGIM_20100710T000000_20100710T235959_0002.DBL");
        addPluginTestDef("DA_TC_CRYOSAT2_COR_IONO_GIM", "cryosat2/gim", fileList,
                         "CS_OPER_AUX_IONGIM_20100710T000000_20100710T235959_0002");

        // FIXME TEST : Pas de donnees de ref ou ??
        // addPluginTestDef("DA_TC_CRYOSAT2_DORIS1B_POE_CDDIS", "cryosat2/doris1b_poe_cddis");

        addPluginTestDef("DA_TC_CRYOSAT2_DORIS1B_POE_CDDIS_COM", "cryosat2/doris1b_poe_cddis_com");

        addPluginTestDef("DA_TC_CRYOSAT2_MOE", "cryosat2/moe");
        addPluginTestDef("DA_TC_CRYOSAT2_MOE_ESA", "cryosat2/moe_pds");
        addPluginTestDef("DA_TC_CRYOSAT2_MOE_EXTRA_4J", "cryosat2/moe_extra");
        addPluginTestDef("DA_TC_CRYOSAT2_POE", "cryosat2/poe");
        addPluginTestDef("DA_TC_CRYOSAT2_POE_ESA", "cryosat2/poe_pds");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_DOR_JAM_0__20100705T042639_20100705T042809_0001.HDR");
        fileList.add("CS_OPER_DOR_JAM_0__20100705T042639_20100705T042809_0001.DBL");
        addPluginTestDef("DA_TC_CRYOSAT2_BROUILLAGE", "cryosat2/tmsci", fileList,
                         "CS_OPER_DOR_JAM_0__20100705T042639_20100705T042809_0001");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_DOR_DAT_0__20100704T102754_20100704T120824_0001.HDR");
        fileList.add("CS_OPER_DOR_DAT_0__20100704T102754_20100704T120824_0001.DBL");
        addPluginTestDef("DA_TC_CRYOSAT2_BULDAT", "cryosat2/tmsci", fileList,
                         "CS_OPER_DOR_DAT_0__20100704T102754_20100704T120824_0001");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_DOR_NAV_0__20100705T093711_20100705T101301_0001.HDR");
        fileList.add("CS_OPER_DOR_NAV_0__20100705T093711_20100705T101301_0001.DBL");
        addPluginTestDef("DA_TC_CRYOSAT2_BULNAV", "cryosat2/tmsci", fileList,
                         "CS_OPER_DOR_NAV_0__20100705T093711_20100705T101301_0001");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_DOR_DOP_0__20100704T102754_20100704T120814_0001.HDR");
        fileList.add("CS_OPER_DOR_DOP_0__20100704T102754_20100704T120814_0001.DBL");
        addPluginTestDef("DA_TC_CRYOSAT2_DOPPLER_DORIS", "cryosat2/tmsci", fileList,
                         "CS_OPER_DOR_DOP_0__20100704T102754_20100704T120814_0001");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_DOR_TST_0__20100704T102754_20100704T120814_0001.HDR");
        fileList.add("CS_OPER_DOR_TST_0__20100704T102754_20100704T120814_0001.DBL");
        addPluginTestDef("DA_TC_CRYOSAT2_DOR_TEST", "cryosat2/tmsci", fileList,
                         "CS_OPER_DOR_TST_0__20100704T102754_20100704T120814_0001");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_TLM_DRTM___20100704T000000_20100704T235959_0001.HDR");
        fileList.add("CS_OPER_TLM_DRTM___20100704T000000_20100704T235959_0001.DBL");
        addPluginTestDef("DA_TC_CRYOSAT2_HKTM_DORIS_BRUTE", "cryosat2/tmsur", fileList,
                         "CS_OPER_TLM_DRTM___20100704T000000_20100704T235959_0001");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_TLM_DCTM___20100704T000000_20100704T235959_0001.HDR");
        fileList.add("CS_OPER_TLM_DCTM___20100704T000000_20100704T235959_0001.DBL");
        addPluginTestDef("DA_TC_CRYOSAT2_HKTM_DORIS_CAL", "cryosat2/tmsur", fileList,
                         "CS_OPER_TLM_DCTM___20100704T000000_20100704T235959_0001");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_TLM_DPTM___20100704T000000_20100704T235959_0001.HDR");
        fileList.add("CS_OPER_TLM_DPTM___20100704T000000_20100704T235959_0001.DBL");
        addPluginTestDef("DA_TC_CRYOSAT2_HKTM_DORIS_PLATEFORM", "cryosat2/tmsur", fileList,
                         "CS_OPER_TLM_DPTM___20100704T000000_20100704T235959_0001");

        // DA_TC_CRYOSAT2_STR
        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_STR1ATT_0__20100705T063000_20100705T064959_0001.DBL");
        fileList.add("CS_OPER_STR1ATT_0__20100705T063000_20100705T064959_0001.HDR");
        addPluginTestDef("DA_TC_CRYOSAT2_STR", "cryosat2/quaternions", fileList,
                         "CS_OPER_STR1ATT_0__20100705T063000_20100705T064959_0001");
        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_STR1DAT_0__20100705T063000_20100705T064959_0001.DBL");
        fileList.add("CS_OPER_STR1DAT_0__20100705T063000_20100705T064959_0001.HDR");
        addPluginTestDef("DA_TC_CRYOSAT2_STR", "cryosat2/quaternions", fileList,
                         "CS_OPER_STR1DAT_0__20100705T063000_20100705T064959_0001");
        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_STR2ATT_0__20100705T063000_20100705T065000_0001.DBL");
        fileList.add("CS_OPER_STR2ATT_0__20100705T063000_20100705T065000_0001.HDR");
        addPluginTestDef("DA_TC_CRYOSAT2_STR", "cryosat2/quaternions", fileList,
                         "CS_OPER_STR2ATT_0__20100705T063000_20100705T065000_0001");
        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_STR2DAT_0__20100705T063000_20100705T064959_0001.DBL");
        fileList.add("CS_OPER_STR2DAT_0__20100705T063000_20100705T064959_0001.HDR");
        addPluginTestDef("DA_TC_CRYOSAT2_STR", "cryosat2/quaternions", fileList,
                         "CS_OPER_STR2DAT_0__20100705T063000_20100705T064959_0001");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_STR3ATT_0__20100705T063000_20100705T065000_0001.DBL");
        fileList.add("CS_OPER_STR3ATT_0__20100705T063000_20100705T065000_0001.HDR");
        addPluginTestDef("DA_TC_CRYOSAT2_STR", "cryosat2/quaternions", fileList,
                         "CS_OPER_STR3ATT_0__20100705T063000_20100705T065000_0001");
        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_STR3DAT_0__20100705T063000_20100705T064959_0001.DBL");
        fileList.add("CS_OPER_STR3DAT_0__20100705T063000_20100705T064959_0001.HDR");
        addPluginTestDef("DA_TC_CRYOSAT2_STR", "cryosat2/quaternions", fileList,
                         "CS_OPER_STR3DAT_0__20100705T063000_20100705T064959_0001");

        // DA_TC_CRYOSAT2_TC_LOGBOOK
        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_REP_DCHF___20081202T000000_20081202T235959_0001.DBL");
        fileList.add("CS_OPER_REP_DCHF___20081202T000000_20081202T235959_0001.HDR");
        addPluginTestDef("DA_TC_CRYOSAT2_TC_LOGBOOK", "cryosat2/acqsol", fileList,
                         "CS_OPER_REP_DCHF___20081202T000000_20081202T235959_0001");
        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_REP_DCHF___20100704T000000_20100704T235959_0001.DBL");
        fileList.add("CS_OPER_REP_DCHF___20100704T000000_20100704T235959_0001.HDR");
        addPluginTestDef("DA_TC_CRYOSAT2_TC_LOGBOOK", "cryosat2/acqsol", fileList,
                         "CS_OPER_REP_DCHF___20100704T000000_20100704T235959_0001");

        addPluginTestDef("DA_TC_CRYOSAT2_HISTO_FOUS_BORD", "cryosat2/fous");

        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_AUX_DORUSO_20100704T073447_20100705T010524_0001.DBL");
        fileList.add("CS_OPER_AUX_DORUSO_20100704T073447_20100705T010524_0001.HDR");
        addPluginTestDef("DA_TC_CRYOSAT2_HISTO_FOUS_BORD_ESA", "cryosat2/fous_pds", fileList,
                         "CS_OPER_AUX_DORUSO_20100704T073447_20100705T010524_0001");

        addPluginTestDef("DA_TC_CRYOSAT2_FREQAJ_BALPUB", "COMMUN/BALISES_PUBLIQUES/FA",
                         "CS__FBP_AXXCNE20100705_042636_20100705_042737_20100706_042838");
        addPluginTestDef("DA_TC_CRYOSAT2_LOGVOL_DORIS_DGXX", "cryosat2/logvol",
                         "LOGICIEL_DORIS_CRYOSAT2_400_306_20071004_180000_EEP.REF");
        addPluginTestDef("DA_TC_CRYOSAT2_PAR_LV_DORIS_DGXX", "cryosat2/logvol", "CS2__1_PAR_DORIS_20070802_123000.REF");
        addPluginTestDef("DA_TC_CRYOSAT2_ZQS_DIODE_DGXX", "cryosat2/logvol", "CS2__1_ZQS_DIODE_20070802_123000.REF");
        addPluginTestDef("DA_TC_CRYOSAT2_ZQS_PAR_SYS_DGXX", "cryosat2/logvol", "CS2__1_ZQS_SYSBD_20070802_123000.REF");
        addPluginTestDef("DA_TC_CRYOSAT2_ZQS_RESEAU_BORD_DGXX", "cryosat2/logvol",
                         "CS2__1_ZQS_BALIS_20070802_123000.REF");
        addPluginTestDef("DA_TC_CRYOSAT2_MESURES_LASER", "cryosat2/laser");

        // CRYOSAT2_HISTO_SOLEIL
        fileList = new ArrayList<>(2);
        fileList.add("CS_OPER_AUX_SUNACT_19910101T000000_20110201T000000_0001.DBL");
        fileList.add("CS_OPER_AUX_SUNACT_19910101T000000_20110201T000000_0001.HDR");
        addPluginTestDef("DA_TC_CRYOSAT2_HISTO_SOLEIL", "COMMUN/SOLEIL/HISTORIQUE", fileList,
                         "CS_OPER_AUX_SUNACT_19910101T000000_20110201T000000_0001");

    }

    @Override
    public void initTestSoloList() {
    }

    @Override
    public String getProjectProperties() {
        return "ssalto/domain/plugins/impl/cryosat2plugin.properties";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
