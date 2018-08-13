/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;

/**
 *
 * @author Christophe Mertz
 *
 */
public class Jason2CheckingPluginTest {

    @Requirement("REGARDS_DSL_ING_SSALTO_070")
    @Purpose("A plugin can generate a SIP for a Jason1's Doris1B products")
    @Test
    public void testProductPlugin() throws ModuleException {
        PluginUtils.setup();

        // Plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(DefaultProductPlugin.FIELD_LENGTH, 128).getParameters();

        // Instantiate plugin
        IProductPlugin plugin = PluginUtils.getPlugin(parameters, DefaultProductPlugin.class, new HashMap<>());
        Assert.assertNotNull(plugin);

        // Run plugin
        Path filePath = Paths
                .get("src/test/resources/income/data/JASON2/IGDR/JA2_IPN_2PcP016_166_20081214_053324_20081214_062937");
        String productName = plugin.getProductName(filePath);
        Assert.assertEquals("JA2_IPN_2PcP016_166_20081214_053324_20081214_062937", productName);
    }

}
