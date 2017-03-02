/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;

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

}
