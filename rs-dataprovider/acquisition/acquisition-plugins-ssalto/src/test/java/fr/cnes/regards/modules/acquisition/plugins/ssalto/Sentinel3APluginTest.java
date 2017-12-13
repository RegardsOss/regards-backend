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
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.SaralProductMetadataPlugin;

/**
 * @author Christophe Mertz
 */
public class Sentinel3APluginTest extends AbstractProductMetadataPluginTest {

    /**
     * Class logger
     */
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
    public IGenerateSIPPlugin buildPlugin() throws ModuleException {
        PluginConfiguration pluginConfiguration = this.getPluginConfiguration("Sent3aProductMetadataPlugin");

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        // addPluginTestDef("DA_TC_SENT3A_COM", "sent3A/com_brut");
        // addPluginTestDef("DA_TC_SENT3A_HISTO_FOUS_ESA", "sent3A/fous_pdgs");

        addPluginTestDef("DA_TC_SENT3A_HISTO_ORF", "sent3A/orf_historique");
        addPluginTestDef("DA_TC_SENT3A_DORIS_NAV", "sent3A/pltm");
    }

    @Override
    public void initTestSoloList() {
    }

    @Override
    public String getProjectProperties() {
        return "ssalto/domain/plugins/impl/sentinal3plugin.properties";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
