/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.dao;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;

/***
 * Unit testing of {@link PluginParameter} persistence.
 *
 * @author Christophe Mertz
 *
 */

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { PluginDaoTestConfig.class })
@DirtiesContext
public class PluginParameterIT extends PluginDaoUtility {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginParameterIT.class);

    @Before
    public void before() {
        injectToken(PROJECT);
        cleanDb();
    }

    /**
     * Unit testing for the creation of a {@link PluginParameter}
     */
    @Test
    public void createPluginParameter() {

        final long nPluginParameter = pluginParameterRepository.count();

        pluginParameterRepository.save(A_PARAMETER);
        pluginParameterRepository.save(PARAMETERS2);

        Assert.assertEquals(nPluginParameter + 1 + PARAMETERS2.size(), pluginParameterRepository.count());

        pluginConfigurationRepository.deleteAll();
    }

    @Test
    @DirtiesContext
    public void createPluginParameterConfiguration() {
        // Create 2 PluginConfiguration
        PluginConfiguration pluginConf1 = pluginConfigurationRepository.save(getPluginConfigurationWithParameters());
        PluginConfiguration pluginConf2 = pluginConfigurationRepository
                .save(getPluginConfigurationWithDynamicParameter());
        Assert.assertEquals(2, pluginConfigurationRepository.count());

        // Create PluginParameter
        List<PluginParameter> params1 = PluginParametersFactory.build()
                .addParameterPluginConfiguration(RED, pluginConf1).getParameters();

        List<PluginParameter> params2 = PluginParametersFactory.build()
                .addParameterPluginConfiguration(BLUE, pluginConf2).getParameters();

        // Create 2 PluginConfiguration with the 2 PluginParameter above
        PluginConfiguration pluginConf3 = pluginConfigurationRepository
                .save(new PluginConfiguration(getPluginMetaData(), "third configuration", params1, 0));
        PluginConfiguration pluginConf4 = pluginConfigurationRepository
                .save(new PluginConfiguration(getPluginMetaData(), "forth configuration", params2, 0));

        List<PluginParameter> params = Lists.newArrayList(pluginParameterRepository.findAll().iterator());

        Assert.assertEquals(pluginConf1.getParameters().size() + pluginConf2.getParameters().size() + 2,
                            pluginParameterRepository.count());

        pluginConfigurationRepository.deleteAll();
        pluginParameterRepository.deleteAll();
    }

    /**
     * Unit testing for the update of a {@link PluginParameter}
     */
    @Test
    @DirtiesContext
    public void updatePluginParameter() {
        pluginParameterRepository.save(INTERFACEPARAMETERS.get(0));
        final PluginParameter paramJpa = pluginParameterRepository.save(PARAMETERS2.get(0));
        Assert.assertEquals(paramJpa.getName(), PARAMETERS2.get(0).getName());

        pluginParameterRepository.findAll().forEach(p -> LOGGER.info(p.getName()));

        pluginParameterRepository.save(paramJpa);

        final PluginParameter paramFound = pluginParameterRepository.findOne(paramJpa.getId());
        Assert.assertEquals(paramFound.getName(), paramJpa.getName());

        pluginParameterRepository.deleteAll();
        pluginConfigurationRepository.deleteAll();
    }

    /**
     * Unit testing for the delete of a {@link PluginParameter}
     */
    @Test
    @DirtiesContext
    public void deletePluginParameter() {
        final PluginParameter paramJpa = pluginParameterRepository.save(A_PARAMETER);
        pluginParameterRepository.save(PARAMETERS2);
        Assert.assertEquals(1 + PARAMETERS2.size(), pluginParameterRepository.count());

        // Delete a plugin parameter
        pluginParameterRepository.delete(paramJpa);
        Assert.assertEquals(PARAMETERS2.size(), pluginParameterRepository.count());

        // Delete a plugin parameter
        pluginParameterRepository.delete(PARAMETERS2.get(0));
        Assert.assertEquals(PARAMETERS2.size() - 1, pluginParameterRepository.count());
    }

    /**
     * Unit testing about the dynamic values of a {@link PluginParameter}
     */
    @Test
    @DirtiesContext
    public void controlPluginParameterDynamicValues() {
        // first plugin parameter
        final PluginParameter savedParam = pluginParameterRepository.save(A_PARAMETER);
        Assert.assertNotNull(savedParam.getId());
        Assert.assertEquals(1, pluginParameterRepository.count());

        // second plugin parameter with dynamic values
        final PluginParameter paramJpa = pluginParameterRepository.save(PARAMETERS2.get(0));
        Assert.assertNotNull(paramJpa.getId());
        Assert.assertEquals(2, pluginParameterRepository.count());

        // search the second plugin parameter
        final PluginParameter paramFound = pluginParameterRepository.findOneWithDynamicsValues(paramJpa.getId());
        paramFound.getDynamicsValues().stream().forEach(p -> LOGGER.info(p.getValue()));

        // test dynamics values of the second parameter
        Assert.assertEquals(paramJpa.isDynamic(), paramFound.isDynamic());
        Assert.assertEquals(paramJpa.getDynamicsValues().size(), paramFound.getDynamicsValues().size());
        Assert.assertEquals(paramJpa.getName(), paramFound.getName());
        Assert.assertEquals(paramJpa.getValue(), paramFound.getValue());
        Assert.assertEquals(paramJpa.getId(), paramFound.getId());
        Assert.assertEquals(paramJpa.getDynamicsValuesAsString().size(), paramFound.getDynamicsValuesAsString().size());
        paramJpa.getDynamicsValuesAsString().stream().forEach(s -> paramFound.getDynamicsValuesAsString().contains(s));
    }

}
