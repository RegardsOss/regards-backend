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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;

/**
 * Test des plugins DORIS1b SPOT2
 *
 * @author Christophe Mertz
 */
public class Spot2Doris1bPluginTest extends AbstractProductMetadataPluginTest {

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
    public IGenerateSIPPlugin buildPlugin() throws ModuleException {
        PluginConfiguration pluginConfiguration = this.getPluginConfiguration("Spot2ProductMetadataPlugin");

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        //        addPluginTestDef("DA_TC_SPOT2_DORIS1B_MOE_CDDIS", "spot2/doris1b_moe_cddis");
        //        addPluginTestDef("DA_TC_SPOT2_DORIS1B_MOE_CDDIS_COM", "spot2/doris1b_moe_cddis_com");
        //        addPluginTestDef("DA_TC_SPOT2_DORIS1B_POE_CDDIS_COM", "spot2/doris1b_poe_cddis_com");
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
