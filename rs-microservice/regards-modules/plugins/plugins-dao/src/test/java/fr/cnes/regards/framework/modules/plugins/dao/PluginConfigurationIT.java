/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.plugins.dao;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

/***
 * Unit testing of {@link PluginConfiguration} persistence.
 *
 * @author Christophe Mertz
 */

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { PluginDaoTestConfig.class })
@DirtiesContext
public class PluginConfigurationIT extends PluginDaoUtility {

    @Before
    public void before() {
        injectToken(PROJECT);
        cleanDb();
    }

    @After
    public void after() {
        injectToken(PROJECT);
        cleanDb();
    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void createPluginConfiguration() {
        // persist a PluginConfiguration
        PluginConfiguration jpaConf = plgRepository.save(getPlgConfWithParameters());

        Assert.assertEquals(1, plgRepository.count());

        Assert.assertEquals(getPlgConfWithParameters().getLabel(), jpaConf.getLabel());
        Assert.assertEquals(getPlgConfWithParameters().getVersion(), jpaConf.getVersion());
        Assert.assertEquals(getPlgConfWithParameters().getPluginId(), jpaConf.getPluginId());
        Assert.assertEquals(getPlgConfWithParameters().isActive(), jpaConf.isActive());
        Assert.assertEquals(getPlgConfWithParameters().getPluginClassName(), jpaConf.getPluginClassName());
        Assert.assertEquals(getPlgConfWithParameters().getPriorityOrder(), jpaConf.getPriorityOrder());
        getPlgConfWithParameters().getParameters().forEach(p -> Assert
                .assertEquals(getPlgConfWithParameters().getParameter(p.getName()), jpaConf.getParameter(p.getName())));

        // persist a PluginConfiguration
        plgRepository.save(getPlgConfWithDynamicParameter());

        Assert.assertEquals(2, plgRepository.count());
    }

    @Test
    public void deletePluginConfigurationWithParameters() {
        // save a plugin configuration
        PluginConfiguration pluginConf = plgRepository.save(getPlgConfWithParameters());
        Assert.assertEquals(1, plgRepository.count());
        // delete it
        plgRepository.deleteById(pluginConf.getId());
        Assert.assertEquals(0, plgRepository.count());
    }

}
