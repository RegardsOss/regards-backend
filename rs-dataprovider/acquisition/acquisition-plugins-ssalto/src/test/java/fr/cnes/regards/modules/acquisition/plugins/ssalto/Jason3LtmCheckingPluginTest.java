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
import fr.cnes.regards.modules.acquisition.plugins.ssalto.check.Jason3LtmP3ProductPlugin;

/**
 *
 * Test des plugins LTM JASON3
 *
 * @author Christophe Mertz
 */
public class Jason3LtmCheckingPluginTest {

    @Requirement("REGARDS_DSL_ING_SSALTO_070")
    @Purpose("A plugin can generate a SIP for a Jason3's LTM products")
    @Test
    public void testProductPlugin() throws ModuleException {
        // Plugin parameters
        List<PluginParameter> parameters = PluginParametersFactory.build().getParameters();

        // Instantiate plugin
        IProductPlugin plugin = PluginUtils.getPlugin(parameters, Jason3LtmP3ProductPlugin.class, new HashMap<>());
        Assert.assertNotNull(plugin);

        // Run plugin
        Path filePath = Paths
                .get("src/test/resources/income/data/JASON3/LTM/PJ3_FI1_AXXCNE20081202_110021_20080615_115927_20081201_120000");
        String productName = plugin.getProductName(filePath);
        Assert.assertEquals("PJ3_I1_AXXCNE20081202_110021_20080615_115927_20081201_120000", productName);
    }
}
