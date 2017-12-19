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
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.acquisition.exception.ReadFileException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.check.Jason2CheckingPlugin;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class Jason2CheckingPluginTest {

    @Requirement("REGARDS_DSL_ING_SSALTO_070")
    @Purpose("A plugin can generate a SIP for a Jason2's IGDR products")
    @Test
    public void testJason2Igdr() throws ModuleException {

        // Parameters
        String fileName = "src/test/resources/income/data/JASON2/IGDR/JA2_IPN_2PcP016_166_20081214_053324_20081214_062937";
        String dataSetId = "DA_TC_JASON2_IGDR";
        // Launch plugin
        Jason2CheckingPlugin plugin = new Jason2CheckingPlugin();
        Assert.assertTrue(plugin.runPlugin(new File(fileName), dataSetId));
        Assert.assertEquals("JA2_IPN_2PcP016_166_20081214_053324_20081214_062937", plugin.getProductName());
    }

    @Requirement("REGARDS_DSL_ING_SSALTO_070")
    @Purpose("A plugin can generate a SIP for a Jason2's IGDR products")
    @Test(expected = ReadFileException.class)
    public void testJason2IgdrFailed() throws ModuleException {

        // Parameters
        String fileName = "src/test/resources/income/data/JASON2/IGDR/JA2_IPN_2PcP016_166_20081214_053324_20081214_099999";
        String dataSetId = "DA_TC_JASON2_IGDR";
        // Launch plugin
        Jason2CheckingPlugin plugin = new Jason2CheckingPlugin();
        Assert.assertTrue(plugin.runPlugin(new File(fileName), dataSetId));
        Assert.fail();
    }

}
