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

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.check.Jason3LtmP3CheckingFilePlugin;

/**
 *
 * Test des plugins LTM JASON3
 *
 * @author Christophe Mertz
 */
public class Jason3LtmCheckingPluginTest {

    @Test
    public void testProduct() throws ModuleException {
        Jason3LtmP3CheckingFilePlugin plugin = new Jason3LtmP3CheckingFilePlugin();

        String fileNameTest = "PJ3_FI1_AXXCNE20081202_110021_20080615_115927_20081201_120000";
        String ProductNameTest = "PJ3_I1_AXXCNE20081202_110021_20080615_115927_20081201_120000";

        File testFile = new File("src/test/resources/income/data/JASON3/LTM", fileNameTest);
        Assert.assertTrue(plugin.runPlugin("chain label", testFile, "DA_TC_JASON3_LTM"));
        Assert.assertEquals(ProductNameTest, plugin.getProductName());
    }

}
