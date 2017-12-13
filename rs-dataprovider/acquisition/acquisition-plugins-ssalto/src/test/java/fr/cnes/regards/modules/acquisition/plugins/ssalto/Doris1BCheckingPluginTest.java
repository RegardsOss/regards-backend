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
import fr.cnes.regards.modules.acquisition.plugins.ssalto.check.Jason1Doris1BCheckingFilePlugin;


/**
 * 
 * @author Christophe Mertz
 *
 */
public class Doris1BCheckingPluginTest {

    @Test
    public void testJason1Doris1B() throws ModuleException {

        // Parameters
        String fileName = "src/test/resources/income/data/spot2/doris1b_moe_cddis/DORDATA_090526.SP2";
        String dataSetId = "DA_TC_JASON1_DORIS1B_MOE_CDDIS";
        // Launch plugin
        Jason1Doris1BCheckingFilePlugin plugin = new Jason1Doris1BCheckingFilePlugin();
            Assert.assertTrue(plugin.runPlugin(new File(fileName), dataSetId));
            Assert.assertEquals("MOE_CDDIS_DORDATA_090526.SP2", plugin.getProductName());
    }

}
