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
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.Jason3Doris10ProductMetadataPlugin;

/**
 * Test des plugins DORIS10 JASON3
 *
 * @author Christophe Mertz
 */
@ContextConfiguration(classes = { PluginsSsaltoTestsConfiguration.class })
@EnableAutoConfiguration
public class Jason3DorisPluginTest extends Jason3PluginTest {

    @Autowired
    IPluginService pluginService;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResoler;

    @Override
    @Before
    public void start() {
        runtimeTenantResoler.forceTenant(DEFAULT_TENANT);
    }

    @Override
    public ISIPGenerationPluginWithMetadataToolbox buildPlugin() throws ModuleException {
        PluginConfiguration pluginConfiguration = this
                .getPluginConfiguration("Jason3Doris10ProductMetadataPlugin",
                                        Optional.of(PluginParametersFactory.build()
                                                .addParameter(Jason3Doris10ProductMetadataPlugin.ORF_FILE_PATH_PARAM,
                                                              "src/test/resources/income/data/JASON3/ORF_HISTORIQUE/JA3_ORF_AXXCNE*")
                                                .addParameter(Jason3Doris10ProductMetadataPlugin.CYCLES_FILE_PATH_PARAM,
                                                              "src/test/resources/income/data/JASON3/CYCLES/JASON3_CYCLES")
                                                .getParameters()));

        return pluginService.getPlugin(pluginConfiguration.getId());
    }

    @Override
    public void initTestList() {
        addPluginTestDef("DA_TC_JASON3_DORIS10_FLAG", "JASON3/DORIS10/DOR10_INVALIDES");
        addPluginTestDef("DA_TC_JASON3_DORIS10_COM", "JASON3/DORIS10/COMMERCIALES_10");
        addPluginTestDef("DA_TC_JASON3_DORIS10_PUB", "JASON3/DORIS10/PUBLIQUES_10");
    }

}
