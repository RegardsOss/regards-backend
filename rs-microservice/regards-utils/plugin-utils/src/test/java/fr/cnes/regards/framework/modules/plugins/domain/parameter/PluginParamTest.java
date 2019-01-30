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
package fr.cnes.regards.framework.modules.plugins.domain.parameter;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter2.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter2.PluginParam;

/**
 * @author Marc SORDI
 *
 */
public class PluginParamTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginParamTest.class);

    @Test
    public void stringParam() {
        PluginParam<String> pp = IPluginParam.buildParam("toto", "tutu");
        LOGGER.debug("Name {}, Class {}", pp.getName(), pp.getClazz());
    }

    @Test
    public void objectParam() {
        PluginParam<PluginConfiguration> pp = IPluginParam.buildParam("toto", new PluginConfiguration());
        LOGGER.debug("Name {}, Class {}", pp.getName(), pp.getClazz());
    }
}
