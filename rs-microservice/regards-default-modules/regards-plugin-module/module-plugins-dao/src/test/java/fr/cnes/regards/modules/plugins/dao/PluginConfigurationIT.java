/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;

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

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void createPluginConfiguration() {

        injectToken(PROJECT);
        cleanDb();

        // persist a PluginConfiguration
        final PluginConfiguration jpaConf = pluginConfigurationRepository.save(getPluginConfigurationWithParameters());

        Assert.assertEquals(1, pluginConfigurationRepository.count());
        Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                            pluginParameterRepository.count());

        // Assert.assertEquals(0, pluginDynamicValueRepository.count());

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
        // Assert.assertEquals(3, pluginDynamicValueRepository.count());

    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void createAndFindPluginConfigurationWithParameters() {
        injectToken(PROJECT);
        cleanDb();

        // save a plugin configuration
        final PluginConfiguration aPluginConf = pluginConfigurationRepository
                .save(getPluginConfigurationWithParameters());
        Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                            pluginParameterRepository.count());
        Assert.assertEquals(1, pluginConfigurationRepository.count());

        // find it
        final PluginConfiguration jpaConf = pluginConfigurationRepository
                .findOneWithPluginParameter(aPluginConf.getId());

        // compare the initial conf with the results of the search
        Assert.assertEquals(aPluginConf.getLabel(), jpaConf.getLabel());
        Assert.assertEquals(aPluginConf.getVersion(), jpaConf.getVersion());
        Assert.assertEquals(aPluginConf.getPluginId(), jpaConf.getPluginId());
        Assert.assertEquals(aPluginConf.isActive(), jpaConf.isActive());
        Assert.assertEquals(aPluginConf.getPluginClassName(), jpaConf.getPluginClassName());
        Assert.assertEquals(aPluginConf.getParameters().size(), pluginParameterRepository.count());
        Assert.assertEquals(aPluginConf.getPriorityOrder(), jpaConf.getPriorityOrder());
        aPluginConf.getParameters().forEach(p -> Assert.assertEquals(aPluginConf.getParameterConfiguration(p.getName()),
                                                                     jpaConf.getParameterConfiguration(p.getName())));

    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void updatePluginConfigurationWithParameters() {
        injectToken(PROJECT);
        cleanDb();

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
    }

    @Test
    public void deletePluginConfigurationWithParameters() {
        injectToken(PROJECT);
        cleanDb();

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
        injectToken(PROJECT);
        cleanDb();

        // save a plugin configuration
        pluginConfigurationRepository.save(getPluginConfigurationWithParameters());
        Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                            pluginParameterRepository.count());
        Assert.assertEquals(1, pluginConfigurationRepository.count());

        Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                            pluginParameterRepository.count());

        // delete it
        pluginParameterRepository.delete(getPluginConfigurationWithParameters().getParameters().get(0));

        Assert.fail();
    }

}
