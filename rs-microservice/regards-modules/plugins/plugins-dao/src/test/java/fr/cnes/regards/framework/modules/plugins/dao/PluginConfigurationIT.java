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
package fr.cnes.regards.framework.modules.plugins.dao;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;

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
        final PluginConfiguration jpaConf = plgRepository.save(getPlgConfWithParameters());

        Assert.assertEquals(1, plgRepository.count());
        Assert.assertEquals(getPlgConfWithParameters().getParameters().size(), paramRepository.count());

        Assert.assertEquals(getPlgConfWithParameters().getLabel(), jpaConf.getLabel());
        Assert.assertEquals(getPlgConfWithParameters().getVersion(), jpaConf.getVersion());
        Assert.assertEquals(getPlgConfWithParameters().getPluginId(), jpaConf.getPluginId());
        Assert.assertEquals(getPlgConfWithParameters().isActive(), jpaConf.isActive());
        Assert.assertEquals(getPlgConfWithParameters().getPluginClassName(), jpaConf.getPluginClassName());
        Assert.assertEquals(getPlgConfWithParameters().getParameters().size(), paramRepository.count());
        Assert.assertEquals(getPlgConfWithParameters().getPriorityOrder(), jpaConf.getPriorityOrder());
        getPlgConfWithParameters().getParameters()
                .forEach(p -> Assert.assertEquals(getPlgConfWithParameters().getParameterConfiguration(p.getName()),
                                                  jpaConf.getParameterConfiguration(p.getName())));

        // persist a PluginConfiguration
        resetId();
        plgRepository.save(getPlgConfWithDynamicParameter());

        Assert.assertEquals(2, plgRepository.count());
        Assert.assertEquals(getPlgConfWithParameters().getParameters().size()
                + getPlgConfWithDynamicParameter().getParameters().size(), paramRepository.count());
    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void createAndFindPluginConfigurationWithParameters() {
        // save a plugin configuration
        PluginConfiguration plgConf = getPlgConfWithParameters();
        final PluginConfiguration aPluginConf = plgRepository.save(plgConf);
        Assert.assertEquals(getPlgConfWithParameters().getParameters().size(), paramRepository.count());
        Assert.assertEquals(1, plgRepository.count());

        plgRepository.save(getPlgConfWithDynamicParameter());

        // find it
        final PluginConfiguration jpaConf = plgRepository.findOneWithPluginParameter(aPluginConf.getId());
        Assert.assertNotNull(jpaConf.getParameters());
        Assert.assertTrue(!jpaConf.getParameters().isEmpty());

        // compare the initial conf with the results of the search
        Assert.assertEquals(aPluginConf.getLabel(), jpaConf.getLabel());
        Assert.assertEquals(aPluginConf.getVersion(), jpaConf.getVersion());
        Assert.assertEquals(aPluginConf.getPluginId(), jpaConf.getPluginId());
        Assert.assertEquals(aPluginConf.isActive(), jpaConf.isActive());
        Assert.assertEquals(aPluginConf.getPluginClassName(), jpaConf.getPluginClassName());
        Assert.assertEquals(aPluginConf.getParameters().size(), getPlgConfWithParameters().getParameters().size());
        Assert.assertEquals(aPluginConf.getPriorityOrder(), jpaConf.getPriorityOrder());

        for (PluginParameter p : aPluginConf.getParameters()) {
            Assert.assertEquals(aPluginConf.getParameterConfiguration(p.getName()),
                                jpaConf.getParameterConfiguration(p.getName()));
        }
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void createTwoPluginConfigurationWithSameLabel() {
        // save a plugin configuration
        plgRepository.save(getPlgConfWithParameters());
        Assert.assertEquals(1, plgRepository.count());

        // try to save a plugin configuration with the same label
        resetId();
        plgRepository.save(getPlgConfWithParameters());

        Assert.fail();
    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void updatePluginConfigurationWithParameters() {
        // save a plugin configuration
        final PluginConfiguration aPluginConf = plgRepository.save(getPlgConfWithParameters());

        // set two new parameters to the plugin configuration
        aPluginConf.setParameters(LIST_PARAMETERS);

        // update the plugin configuration
        final PluginConfiguration jpaConf = plgRepository.save(aPluginConf);

        Assert.assertEquals(1, plgRepository.count());

        // compare the initial conf with the results of the search
        Assert.assertEquals(aPluginConf.getLabel(), jpaConf.getLabel());
        Assert.assertEquals(aPluginConf.getVersion(), jpaConf.getVersion());
        Assert.assertEquals(aPluginConf.getPluginId(), jpaConf.getPluginId());
        Assert.assertEquals(aPluginConf.isActive(), jpaConf.isActive());
        Assert.assertEquals(aPluginConf.getPluginClassName(), jpaConf.getPluginClassName());
        Assert.assertEquals(aPluginConf.getPriorityOrder(), jpaConf.getPriorityOrder());
        aPluginConf.getParameters().forEach(p -> Assert.assertEquals(aPluginConf.getParameterConfiguration(p.getName()),
                                                                     jpaConf.getParameterConfiguration(p.getName())));

        INTERFACEPARAMETERS.forEach(p -> paramRepository.delete(p));
        Assert.assertEquals(aPluginConf.getParameters().size(), paramRepository.count());
    }

    @Test
    public void deletePluginConfigurationWithParameters() {
        // save a plugin configuration
        final PluginConfiguration aPluginConf = plgRepository.save(getPlgConfWithParameters());
        Assert.assertEquals(getPlgConfWithParameters().getParameters().size(), paramRepository.count());
        Assert.assertEquals(1, plgRepository.count());

        Assert.assertEquals(getPlgConfWithParameters().getParameters().size(), paramRepository.count());

        // delete it
        plgRepository.delete(aPluginConf.getId());

        Assert.assertEquals(0, plgRepository.count());
        Assert.assertEquals(0, paramRepository.count());
    }

    @Test
    public void deleteAPluginParameter() {
        // save a plugin configuration
        PluginConfiguration aPluginConf = plgRepository.save(getPlgConfWithParameters());
        Assert.assertEquals(aPluginConf.getParameters().size(), paramRepository.count());
        Assert.assertEquals(1, plgRepository.count());

        // delete a parameter
        paramRepository.delete(aPluginConf.getParameters().get(0));

        Assert.assertEquals(1, plgRepository.count());
        Assert.assertEquals(aPluginConf.getParameters().size() - 1, paramRepository.count());
    }

    @Test
    public void createPluginParameterConfiguration() {
        int nbPlgConfs = 0;

        // Create 2 PluginConfiguration
        PluginConfiguration pluginConf1 = plgRepository.save(getPlgConfWithParameters());
        PluginConfiguration pluginConf2 = plgRepository.save(getPlgConfWithDynamicParameter());
        nbPlgConfs = 2;

        Assert.assertEquals(2, plgRepository.count());
        Assert.assertEquals(pluginConf1.getParameters().size() + pluginConf2.getParameters().size(),
                            paramRepository.count());

        // Create PluginParameter
        List<PluginParameter> params1 = PluginParametersFactory.build().addPluginConfiguration(RED, pluginConf1)
                .getParameters();

        List<PluginParameter> params2 = PluginParametersFactory.build().addPluginConfiguration(BLUE, pluginConf2)
                .getParameters();

        // Create 2 PluginConfiguration with the 2 PluginParameter above
        plgRepository.save(new PluginConfiguration(getPluginMetaData(), "third configuration", params1, 0));
        plgRepository.save(new PluginConfiguration(getPluginMetaData(), "forth configuration", params2, 0));
        nbPlgConfs += 2;

        Assert.assertEquals(pluginConf1.getParameters().size() + pluginConf2.getParameters().size() + 2,
                            paramRepository.count());
        Assert.assertEquals(nbPlgConfs, plgRepository.count());
    }

    @Test
    @DirtiesContext
    public void createPluginParameterConfigurationWithIdenticalParams() {
        int nbPlgConfs = 0;

        // Create 2 PluginConfiguration
        PluginConfiguration pluginConf1 = plgRepository.save(getPlgConfWithParameters());
        PluginConfiguration pluginConf2 = plgRepository.save(getPlgConfWithDynamicParameter());
        nbPlgConfs = 2;
        Assert.assertEquals(pluginConf1.getParameters().size() + pluginConf2.getParameters().size(),
                            paramRepository.count());
        Assert.assertEquals(nbPlgConfs, plgRepository.count());

        // Create PluginParameter
        List<PluginParameter> params1 = PluginParametersFactory.build().addPluginConfiguration(RED, pluginConf1)
                .getParameters();

        // Create 2 PluginConfiguration with the same PluginParameter
        plgRepository.save(new PluginConfiguration(getPluginMetaData(), "third configuration", params1, 0));
        PluginConfiguration plgConf = new PluginConfiguration(getPluginMetaData(), "forth configuration", null, 0);
        plgRepository.save(plgConf);
        plgConf.setParameters(params1);
        plgRepository.save(plgConf);

        nbPlgConfs += 2;
        Assert.assertEquals(pluginConf1.getParameters().size() + pluginConf2.getParameters().size() + 1,
                            paramRepository.count());
        Assert.assertEquals(nbPlgConfs, plgRepository.count());
    }

}
