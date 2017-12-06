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
package fr.cnes.regards.modules.ingest.service.chain;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.ingest.service.plugin.FakeSipValidation;

/**
 * Test {@link FakeSipValidationTest} plugin
 *
 * @author Marc Sordi
 *
 */
public class FakeSipValidationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FakeSipValidationTest.class);

    private static final String MODULE_PACKAGE = "fr.cnes.regards.modules.ingest";

    private final Gson gson = new Gson();

    @Test
    public void buildMetadata() {
        PluginMetaData mtd = PluginUtils.createPluginMetaData(FakeSipValidation.class, MODULE_PACKAGE);
        Assert.assertNotNull(mtd);
        LOGGER.debug(gson.toJson(mtd));
    }
}
