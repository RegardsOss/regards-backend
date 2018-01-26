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
 * Test des plugins SPOT5
 *
 * @author Christophe Mertz
 */
@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@EnableAutoConfiguration
public class Spot5PluginTest extends AbstractProductMetadataPluginTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Spot5PluginTest.class);

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
        PluginConfiguration pluginConfiguration = this.getPluginConfiguration("Spot5ProductMetadataPlugin");

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_SPOT5_ARCS_POE", "spot5/arcs");
        addPluginTestDef("DA_TC_SPOT5_HISTO_MANOEUVRES", "spot5/manoeuvres");
        addPluginTestDef("DA_TC_SPOT5_HISTO_COM", "spot5/com");
        addPluginTestDef("DA_TC_SPOT5_DORIS10_COM", "spot5/commerciales_10");
        addPluginTestDef("DA_TC_SPOT5_DORIS10_PUB", "spot5/publiques_10");

        addPluginTestDef("DA_TC_SPOT5_DORIS1B_MOE_CDDIS", "spot5/d1b_moe_cddis");
        addPluginTestDef("DA_TC_SPOT5_DORIS1B_MOE_CDDIS_COM", "spot5/d1b_moe_cddis_cm");
        // addPluginTestDef("DA_TC_SPOT5_DORIS1B_POE_CDDIS", "spot5/d1b_poe_cddis");

        addPluginTestDef("DA_TC_SPOT5_MOE", "spot5/moe");
        addPluginTestDef("DA_TC_SPOT5_POE", "spot5/poe");
        addPluginTestDef("DA_TC_SPOT5_PLTM", "spot5/tmsci_nomi");
        addPluginTestDef("DA_TC_SPOT5_PLTM", "spot5/tmsci_redo");
        addPluginTestDef("DA_TC_SPOT5_HKTM", "spot5/tmsur");
        addPluginTestDef("DA_TC_SPOT5_FREQAJ_BALPUB", "COMMUN/BALISES_PUBLIQUES/FA",
                         "SP5_FBP_AXXCNE20090617_142800_20090616_020225_20090617_015455");
        addPluginTestDef("DA_TC_SPOT5_LOGVOL_DORIS_2GM", "spot5/logvol");
        addPluginTestDef("DA_TC_SPOT5_ZQS_DIODE_2GM", "spot5/logvol/z_quasi");

        // no data available
        // addPluginTestDef("DA_TC_SPOT5_POE_SP3", "spot5/poe_sp3");
    }

    @Override
    public void initTestSoloList() {
        // addPluginTestDef("DA_TC_SPOT5_ZQS_DIODE_2GM", "spot5/logvol/z_quasi", "z_quasi_stat_DIODE_SPOT5_12");
    }

    @Override
    public String getProjectProperties() {
        return "ssalto/domain/plugins/impl/spot5plugin.properties";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
