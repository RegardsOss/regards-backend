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
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.modules.acquisition.plugins.ISIPGenerationPluginWithMetadataToolbox;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.Hy2ADoris10ProductMetadataPlugin;

/**
 * @author Christophe Mertz
 */
public class Hy2ADorisPluginTest extends AbstractProductMetadataPluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hy2ADorisPluginTest.class);

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
                .getPluginConfiguration("Hy2ADoris10ProductMetadataPlugin",
                                        Optional.of(PluginParametersFactory.build()
                                                .addParameter(Hy2ADoris10ProductMetadataPlugin.ORF_FILE_PATH_PARAM,
                                                              "src/test/resources/income/data/HY2A/ORF_HISTORIQUE/H2A_ORF_AXXCNE*")
                                                .getParameters()));

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_HY2A_DORIS10_COM", "HY2A/COMMERCIALES_10");
        addPluginTestDef("DA_TC_HY2A_DORIS10_FLAG", "HY2A/DOR10_INVALIDES");
        addPluginTestDef("DA_TC_HY2A_DORIS10_PUB", "HY2A/PUBLIQUES_10");
    }

    @Override
    public void initTestSoloList() {
        addPluginTestDef("DA_TC_HY2A_DORIS10_PUB", "HY2A/PUBLIQUES_10",
                         "DH2A_MEP_1PaS20091211_164309_20010821_000005_20010821_085819");
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
