/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.dao;

import java.util.List;

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
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;

/***
 * Unit testing of {@link PluginConfiguration} persistence.
 *
 * @author Christophe Mertz
 *
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

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void createPluginConfiguration() {
        // persist a PluginConfiguration
        final PluginConfiguration jpaConf = pluginConfigurationRepository.save(getPluginConfigurationWithParameters());

        Assert.assertEquals(1, pluginConfigurationRepository.count());
        Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                            pluginParameterRepository.count());

        Assert.assertEquals(getPluginConfigurationWithParameters().getLabel(), jpaConf.getLabel());
        Assert.assertEquals(getPluginConfigurationWithParameters().getVersion(), jpaConf.getVersion());
        Assert.assertEquals(getPluginConfigurationWithParameters().getPluginId(), jpaConf.getPluginId());
        Assert.assertEquals(getPluginConfigurationWithParameters().isActive(), jpaConf.isActive());
        Assert.assertEquals(getPluginConfigurationWithParameters().getPluginClassName(), jpaConf.getPluginClassName());
        Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                            pluginParameterRepository.count());
        Assert.assertEquals(getPluginConfigurationWithParameters().getPriorityOrder(), jpaConf.getPriorityOrder());
        getPluginConfigurationWithParameters().getParameters()
                .forEach(p -> Assert
                        .assertEquals(getPluginConfigurationWithParameters().getParameterConfiguration(p.getName()),
                                      jpaConf.getParameterConfiguration(p.getName())));

        // persist a PluginConfiguration
        resetId();
        pluginConfigurationRepository.save(getPluginConfigurationWithDynamicParameter());

        Assert.assertEquals(2, pluginConfigurationRepository.count());
        Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size()
                + getPluginConfigurationWithDynamicParameter().getParameters().size(),
                            pluginParameterRepository.count());

        pluginConfigurationRepository.deleteAll();
    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void createAndFindPluginConfigurationWithParameters() {
        // save a plugin configuration
        final PluginConfiguration aPluginConf = pluginConfigurationRepository
                .save(getPluginConfigurationWithParameters());
        Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                            pluginParameterRepository.count());
        Assert.assertEquals(1, pluginConfigurationRepository.count());

        // find it
        final PluginConfiguration jpaConf = pluginConfigurationRepository
                .findOneWithPluginParameter(aPluginConf.getId());
        Assert.assertNotNull(jpaConf.getParameters());
        Assert.assertTrue(!jpaConf.getParameters().isEmpty());

        // compare the initial conf with the results of the search
        Assert.assertEquals(aPluginConf.getLabel(), jpaConf.getLabel());
        Assert.assertEquals(aPluginConf.getVersion(), jpaConf.getVersion());
        Assert.assertEquals(aPluginConf.getPluginId(), jpaConf.getPluginId());
        Assert.assertEquals(aPluginConf.isActive(), jpaConf.isActive());
        Assert.assertEquals(aPluginConf.getPluginClassName(), jpaConf.getPluginClassName());
        Assert.assertEquals(aPluginConf.getParameters().size(), pluginParameterRepository.count());
        Assert.assertEquals(aPluginConf.getPriorityOrder(), jpaConf.getPriorityOrder());

        for (PluginParameter p : aPluginConf.getParameters()) {
            Assert.assertEquals(aPluginConf.getParameterConfiguration(p.getName()),
                                jpaConf.getParameterConfiguration(p.getName()));
        }
        pluginConfigurationRepository.deleteAll();
    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void updatePluginConfigurationWithParameters() {
        // save a plugin configuration
        final PluginConfiguration aPluginConf = pluginConfigurationRepository
                .save(getPluginConfigurationWithParameters());

        // set two new parameters to the plugin configuration
        aPluginConf.setParameters(PARAMETERS2);

        // update the plugin configuration
        final PluginConfiguration jpaConf = pluginConfigurationRepository.save(aPluginConf);

        Assert.assertEquals(1, pluginConfigurationRepository.count());

        // compare the initial conf with the results of the search
        Assert.assertEquals(aPluginConf.getLabel(), jpaConf.getLabel());
        Assert.assertEquals(aPluginConf.getVersion(), jpaConf.getVersion());
        Assert.assertEquals(aPluginConf.getPluginId(), jpaConf.getPluginId());
        Assert.assertEquals(aPluginConf.isActive(), jpaConf.isActive());
        Assert.assertEquals(aPluginConf.getPluginClassName(), jpaConf.getPluginClassName());
        Assert.assertEquals(aPluginConf.getPriorityOrder(), jpaConf.getPriorityOrder());
        aPluginConf.getParameters().forEach(p -> Assert.assertEquals(aPluginConf.getParameterConfiguration(p.getName()),
                                                                     jpaConf.getParameterConfiguration(p.getName())));

        INTERFACEPARAMETERS.forEach(p -> pluginParameterRepository.delete(p));
        Assert.assertEquals(aPluginConf.getParameters().size(), pluginParameterRepository.count());

        pluginConfigurationRepository.deleteAll();
    }

    @Test
    public void deletePluginConfigurationWithParameters() {
        // save a plugin configuration
        final PluginConfiguration aPluginConf = pluginConfigurationRepository
                .save(getPluginConfigurationWithParameters());
        Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                            pluginParameterRepository.count());
        Assert.assertEquals(1, pluginConfigurationRepository.count());

        Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                            pluginParameterRepository.count());

        // delete it
        pluginConfigurationRepository.delete(aPluginConf.getId());

        Assert.assertEquals(0, pluginConfigurationRepository.count());
        Assert.assertEquals(0, pluginParameterRepository.count());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void deletePluginConfigurationWithParametersError() {
        // save a plugin configuration
        pluginConfigurationRepository.save(getPluginConfigurationWithParameters());
        Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                            pluginParameterRepository.count());
        Assert.assertEquals(1, pluginConfigurationRepository.count());

        Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                            pluginParameterRepository.count());

        // delete it
        try {
            pluginParameterRepository.delete(getPluginConfigurationWithParameters().getParameters().get(0));
        } catch (DataIntegrityViolationException e) {
            pluginConfigurationRepository.deleteAll();
            throw e;
        }

        Assert.fail();
    }

    @Test
    public void createPluginParameterConfiguration() {
        int nbPlgConfs = 0;

        // Create 2 PluginConfiguration
        PluginConfiguration pluginConf1 = pluginConfigurationRepository.save(getPluginConfigurationWithParameters());
        PluginConfiguration pluginConf2 = pluginConfigurationRepository
                .save(getPluginConfigurationWithDynamicParameter());
        nbPlgConfs = 2;

        Assert.assertEquals(2, pluginConfigurationRepository.count());
        Assert.assertEquals(pluginConf1.getParameters().size() + pluginConf2.getParameters().size(),
                            pluginParameterRepository.count());

        // Create PluginParameter
        List<PluginParameter> params1 = PluginParametersFactory.build()
                .addParameterPluginConfiguration(RED, pluginConf1).getParameters();

        List<PluginParameter> params2 = PluginParametersFactory.build()
                .addParameterPluginConfiguration(BLUE, pluginConf2).getParameters();

        // Create 2 PluginConfiguration with the 2 PluginParameter above
        pluginConfigurationRepository
                .save(new PluginConfiguration(getPluginMetaData(), "third configuration", params1, 0));
        pluginConfigurationRepository
                .save(new PluginConfiguration(getPluginMetaData(), "forth configuration", params2, 0));
        nbPlgConfs += 2;

        Assert.assertEquals(pluginConf1.getParameters().size() + pluginConf2.getParameters().size() + 2,
                            pluginParameterRepository.count());
        Assert.assertEquals(nbPlgConfs, pluginConfigurationRepository.count());
    }

    @Test
    @DirtiesContext
    public void createPluginParameterConfigurationWithIdenticalParams() {
        int nbPlgConfs = 0;

        // Create 2 PluginConfiguration
        PluginConfiguration pluginConf1 = pluginConfigurationRepository.save(getPluginConfigurationWithParameters());
        PluginConfiguration pluginConf2 = pluginConfigurationRepository
                .save(getPluginConfigurationWithDynamicParameter());
        nbPlgConfs = 2;
        Assert.assertEquals(pluginConf1.getParameters().size() + pluginConf2.getParameters().size(),
                            pluginParameterRepository.count());
        Assert.assertEquals(nbPlgConfs, pluginConfigurationRepository.count());

        // Create PluginParameter
        List<PluginParameter> params1 = PluginParametersFactory.build()
                .addParameterPluginConfiguration(RED, pluginConf1).getParameters();

        // Create 2 PluginConfiguration with the same PluginParameter
        pluginConfigurationRepository
                .save(new PluginConfiguration(getPluginMetaData(), "third configuration", params1, 0));
        PluginConfiguration plgConf = new PluginConfiguration(getPluginMetaData(), "forth configuration", null, 0);
        pluginConfigurationRepository.save(plgConf);
        plgConf.setParameters(params1);
        pluginConfigurationRepository.save(plgConf);

        nbPlgConfs += 2;
        Assert.assertEquals(pluginConf1.getParameters().size() + pluginConf2.getParameters().size() + 1,
                            pluginParameterRepository.count());
        Assert.assertEquals(nbPlgConfs, pluginConfigurationRepository.count());
    }

}
