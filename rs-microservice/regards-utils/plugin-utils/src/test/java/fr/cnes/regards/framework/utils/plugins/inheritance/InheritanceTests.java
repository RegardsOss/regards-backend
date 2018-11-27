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
package fr.cnes.regards.framework.utils.plugins.inheritance;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;

/**
 * Test plugin with inheritance
 * @author Marc Sordi
 */
public class InheritanceTests {

    @Test
    public void test() {

        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(BasicPlugin.FIELD_NAME_STRING, "hello!")
                .addParameter(BasicPlugin.INHERITED_FIELD_NAME_STRING, "inherited hello!").getParameters();

        PluginUtils.setup(this.getClass().getPackage().getName());
        IBasicPlugin plugin = PluginUtils.getPlugin(parameters, BasicPlugin.class, null);
        Assert.assertNotNull(plugin);
        plugin.doIt();
    }
}
