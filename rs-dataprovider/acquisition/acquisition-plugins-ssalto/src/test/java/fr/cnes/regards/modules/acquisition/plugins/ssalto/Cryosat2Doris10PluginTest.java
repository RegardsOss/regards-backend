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

import com.google.common.base.Optional;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.modules.acquisition.plugins.ISIPGenerationPluginWithMetadataToolbox;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.AbstractProductMetadataPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.Cryosat2Doris10ProductMetadataPlugin;

/**
 * Test des plugins CRYOSAT2 de niveau produit
 *
 * @author Christophe Mertz
 */
public class Cryosat2Doris10PluginTest extends AbstractProductMetadataPluginTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Jason2PluginTest.class);

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResoler;

    @Before
    public void before() {
        tenantResolver.forceTenant(DEFAULT_TENANT);
    }

    @Before
    public void start() {
        runtimeTenantResoler.forceTenant(DEFAULT_TENANT);
    }

    @Override
    public ISIPGenerationPluginWithMetadataToolbox buildPlugin(String datasetName) throws ModuleException {
        PluginConfiguration pluginConfiguration = getPluginConfiguration("Cryosat2Doris10ProductMetadataPlugin",
                                                                         Optional.of(PluginParametersFactory.build()
                                                                                 .addParameter(AbstractProductMetadataPlugin.DATASET_SIP_ID,
                                                                                               datasetName)
                                                                                 .addParameter(Cryosat2Doris10ProductMetadataPlugin.ORF_FILE_PATH_PARAM,
                                                                                               "src/test/resources/income/data/cryosat2/orf/CS__ORF_AXXCNE*")
                                                                                 .getParameters()));

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_CRYOSAT2_DORIS10_COM", "cryosat2/commerciales_10");
        addPluginTestDef("DA_TC_CRYOSAT2_DORIS10_PUB", "cryosat2/publiques_10");
        addPluginTestDef("DA_TC_CRYOSAT2_DORIS10_FLAG", "cryosat2/dor10_invalides");
    }

    @Override
    public void createMetadataPlugin_solo() {
        super.createMetadataPlugin_solo();
    }

    @Override
    public void initTestSoloList() {
        addPluginTestDef("DA_TC_CRYOSAT2_DORIS10_FLAG", "cryosat2/dor10_invalides");
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
