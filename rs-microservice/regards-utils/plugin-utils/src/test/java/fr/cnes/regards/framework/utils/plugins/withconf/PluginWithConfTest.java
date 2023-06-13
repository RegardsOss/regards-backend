/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.plugins.withconf;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;

/**
 * Test class for {@link PluginUtils#doInitPlugin(Object, PluginConfiguration)} with hasConfiguration=true in
 * {@link fr.cnes.regards.framework.modules.plugins.annotations.PluginInit}}
 *
 * @author Thibaud Michaudel
 **/
public class PluginWithConfTest {

    @Test
    public void pluginWithConfTest() {
        String businessId = "MyIncrediblePlugin";

        PluginConfiguration conf = new PluginConfiguration("",
                                                           new HashSet<>(),
                                                           PluginWithConf.class.getAnnotation(Plugin.class).id());
        conf.setBusinessId(businessId);

        PluginUtils.setup(this.getClass().getPackage().getName());
        PluginWithConf plugin = PluginUtils.getPlugin(conf, PluginWithConf.class.getCanonicalName(), null);
        Assert.assertNotNull(plugin);
        Assert.assertEquals(businessId, plugin.getPluginInstanceName());
    }
}
