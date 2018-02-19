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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.EnvisatProductMetadataPlugin;

@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@EnableAutoConfiguration
public class EnvisatPluginTest extends AbstractProductMetadataPluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvisatPluginTest.class);

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
                .getPluginConfiguration("EnvisatProductMetadataPlugin", Optional.of(PluginParametersFactory.build()
                        .addParameter(EnvisatProductMetadataPlugin.CYCLES_FILE_PATH_PARAM,
                                      "src/test/resources/income/data/ENVISAT/cycles/ENVISAT_CYCLES")
                        .addParameter(EnvisatProductMetadataPlugin.ORF_FILE_PATH_PARAM,
                                      "src/test/resources/income/data/ENVISAT/orf/EN1_ORF_AXXCNE*")
                        .getParameters()));

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_ENVISAT_CYCLES", "ENVISAT/cycles");
        addPluginTestDef("DA_TC_ENVISAT_HISTO_ORF", "ENVISAT/orf");
        addPluginTestDef("DA_TC_ENVISAT_CORPFALTI_MOE", "ENVISAT/corpfalti_moe");
        addPluginTestDef("DA_TC_ENVISAT_CORPFALTI_POE", "ENVISAT/corpfalti_poe");
        addPluginTestDef("DA_TC_ENVISAT_HISTO_MANOEUVRES", "ENVISAT/manoeuvre");
        addPluginTestDef("DA_TC_ENVISAT_HISTO_COM", "ENVISAT/com");
        addPluginTestDef("DA_TC_ENVISAT_XNG_7J", "ENVISAT/xng_7j");
        addPluginTestDef("DA_TC_ENVISAT_XNG_CYCLE", "ENVISAT/xng_35j");
        addPluginTestDef("DA_TC_ENVISAT_COR_IONO_GIM", "ENVISAT/gim");
        addPluginTestDef("DA_TC_ENVISAT_TEC", "ENVISAT/tec");
        addPluginTestDef("DA_TC_ENVISAT_DORIS10_COM", "ENVISAT/commerciales_10");
        addPluginTestDef("DA_TC_ENVISAT_DORIS10_PUB", "ENVISAT/publiques_10");
        addPluginTestDef("DA_TC_ENVISAT_ORBNAV_FLOT", "ENVISAT/navigateur_flot");
        addPluginTestDef("DA_TC_ENVISAT_ORBNAV_JOUR", "ENVISAT/navigateur_jour");
        addPluginTestDef("DA_TC_ENVISAT_DORIS1B_MOE_CDDIS", "ENVISAT/doris1b_moe_cddis");
        // pas de data addPluginTestDef("DA_TC_ENVISAT_DORIS1B_POE_CDDIS", "ENVISAT/doris1b_poe_cddis");
        addPluginTestDef("DA_TC_ENVISAT_MOE", "ENVISAT/moe_cma");
        addPluginTestDef("DA_TC_ENVISAT_MOE_EXTRA_4J", "ENVISAT/moe_extra");

        addPluginTestDef("DA_TC_ENVISAT_HKTM", "ENVISAT/tmsur");
        addPluginTestDef("DA_TC_ENVISAT_SELECTED_HKTMR_QUATERNION", "ENVISAT/quaternions");

        addPluginTestDef("DA_TC_ENVISAT_FREQAJ_BALPUB", "COMMUN/BALISES_PUBLIQUES/FA",
                         "EN1_FBP_AXXCNE20090611_014700_20090609_020305_20090610_015725");
        addPluginTestDef("DA_TC_ENVISAT_LOGVOL_DORIS_2G", "ENVISAT/logvol", "GESTION_DORIS_ENVISAT160030");
        addPluginTestDef("DA_TC_ENVISAT_ZQS_DORIS_2G", "ENVISAT/logvol", "z_quasi_stat_DORIS_ENVISAT1_22");
        addPluginTestDef("DA_TC_ENVISAT_ZQS_DIODE_2G", "ENVISAT/logvol", "z_quasi_stat_DIODE_ENVISAT1_22");

        addPluginTestDef("DA_TC_ENVISAT_MESURES_LASER", "ENVISAT/laser");
        addPluginTestDef("DA_TC_ENVISAT_HISTO_FOUS_BORD", "ENVISAT/fous");
        addPluginTestDef("DA_TC_ENVISAT_POE", "ENVISAT/poe");
        addPluginTestDef("DA_TC_ENVISAT_ARCS_POE", "ENVISAT/arcs");

        // pas de data
        // addPluginTestDef("DA_TC_ENVISAT_MOE_CCI", "ENVISAT/moe_cci");
        // addPluginTestDef("DA_TC_ENVISAT_MOE_PREDICTED_CCI", "ENVISAT/moe_cma");
        addPluginTestDef("DA_TC_ENVISAT_USO_CORRECTION_GDR", "ENVISAT/gdr");

    }

    @Override
    public void initTestSoloList() {
    }

    @Test
    public void parseDateInsensitiveCase() {
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive()
                .appendPattern("dd-MMM-yyyy HH:mm:ss").toFormatter().withLocale(Locale.US);
        String dateStr = "11-MAY-2009 02:00:44";
        try {
            LocalDateTime ld = LocalDateTime.parse(dateStr, dateTimeFormatter);
            Assert.assertNotNull(ld);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    @Test
    public void parseAndFormatDate() {
        DateTimeFormatter dateTimeFormatterToParse = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        String dateStr = "11-05-2009 02:00:44";
        LocalDateTime ld = LocalDateTime.parse(dateStr, dateTimeFormatterToParse);
        Assert.assertNotNull(ld);
        Assert.assertEquals(11, ld.getDayOfMonth());
        Assert.assertEquals(5, ld.getMonthValue());
        Assert.assertEquals(2009, ld.getYear());
        Assert.assertEquals(2, ld.getHour());
        Assert.assertEquals(0, ld.getMinute());
        Assert.assertEquals(44, ld.getSecond());

        DateTimeFormatter dateTimeFormatterToFormat = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")
                .withLocale(Locale.US);
        String newDate = ld.format(dateTimeFormatterToFormat);
        Assert.assertNotNull(newDate);

        LOGGER.info("------>" + newDate);
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
