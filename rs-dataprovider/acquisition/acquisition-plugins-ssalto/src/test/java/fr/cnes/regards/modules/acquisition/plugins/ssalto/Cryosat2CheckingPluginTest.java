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

/**
 * Test des plugins CRYOSAT2 de niveau produit
 * 
 * @author Christophe Mertz
 */
public class Cryosat2CheckingPluginTest {

    @Test
    public void testWithDblExtension() throws ModuleException {
        // Parameters
        String fileName = "src/test/resources/income/data/cryosat2/manoeuvres/CS_OPER_REP_MACP___20100711T145514_99999999T999999_0001.DBL";
        String dataSetId = "DA_TC_CRYOSAT2_MANOEUVRES_ACP";

        // Launch plugin
        Cryosat2ExtCheckingFilePlugin plugin = new Cryosat2ExtCheckingFilePlugin();
        Assert.assertTrue(plugin.runPlugin(new File(fileName), dataSetId));
        Assert.assertEquals("CS_OPER_REP_MACP___20100711T145514_99999999T999999_0001", plugin.getProductName());
        Assert.assertEquals("CS_OPER_REP_MACP___20100711T145514_99999999T999999_0001.DBL", plugin.getNodeIdentifier());
        Assert.assertEquals(1, plugin.getFileVersion());
        Assert.assertEquals(1, plugin.getProductVersion());
        Assert.assertNull(plugin.getLogFile());
    }

    @Test
    public void testWithHdrExtension() throws ModuleException {
        // Parameters
        String fileName = "src/test/resources/income/data/cryosat2/manoeuvres/CS_OPER_REP_MACP___20100711T145514_99999999T999999_0001.HDR";
        String dataSetId = "DA_TC_CRYOSAT2_MANOEUVRES_ACP";

        // Launch plugin
        Cryosat2ExtCheckingFilePlugin plugin = new Cryosat2ExtCheckingFilePlugin();
        Assert.assertTrue(plugin.runPlugin(new File(fileName), dataSetId));
        Assert.assertEquals("CS_OPER_REP_MACP___20100711T145514_99999999T999999_0001", plugin.getProductName());
        Assert.assertEquals("CS_OPER_REP_MACP___20100711T145514_99999999T999999_0001.HDR", plugin.getNodeIdentifier());
        Assert.assertEquals(1, plugin.getFileVersion());
        Assert.assertEquals(1, plugin.getProductVersion());
        Assert.assertNull(plugin.getLogFile());
    }
}
