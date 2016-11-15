/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.service;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginDynamicValue;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 *
 * Unit testing of {@link PluginService}.
 *
 * @author Christophe Mertz
 */
public class PluginServiceUpdateDynamicParameterTest extends PluginServiceUtility {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginServiceUpdateDynamicParameterTest.class);

    /**
     * 
     */
    private IPluginConfigurationRepository pluginConfRepositoryMocked;

    /**
     * 
     */
    private IPluginService pluginServiceMocked;

    /**
     * This method is run before all tests
     */
    @Before
    public void init() {
        // create a mock repository
        pluginConfRepositoryMocked = Mockito.mock(IPluginConfigurationRepository.class);
        pluginServiceMocked = new PluginService(pluginConfRepositoryMocked);
    }

    /**
     * Update a {@link PluginConfiguration} : change the parameter's status from dynamic to not dynamic
     */
    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_330")
    @Purpose("Change the parameter's status from dynamic to not dynamic.")
    public void updateDynamicParameter() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setId(AN_ID);
        try {
            Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);
            Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId()))
                    .thenReturn(aPluginConfiguration);
            Mockito.when(pluginConfRepositoryMocked.save(aPluginConfiguration)).thenReturn(aPluginConfiguration);

            PluginConfiguration updatedConf = pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);

            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(p -> p.isDynamic()).count(),
                                updatedConf.getParameters().stream().filter(p -> p.isDynamic()).count());
            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(p -> !p.isDynamic()).count(),
                                updatedConf.getParameters().stream().filter(p -> !p.isDynamic()).count());

            aPluginConfiguration.logParams();
            final List<PluginParameter> parameters = aPluginConfiguration.getParameters();
            for (PluginParameter p : updatedConf.getParameters()) {
                if (p.isDynamic()) {
                    if (!p.getDynamicsValuesAsString().isEmpty()) {
                        parameters.remove(p);
                        p.setIsDynamic(false);
                        p.getDynamicsValues().removeAll(p.getDynamicsValues());
                        parameters.add(p);
                        break;
                    }
                }
            }

            aPluginConfiguration.setParameters(parameters);
            updatedConf = pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);

            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(p -> p.isDynamic()).count(),
                                updatedConf.getParameters().stream().filter(p -> p.isDynamic()).count());
            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(p -> !p.isDynamic()).count(),
                                updatedConf.getParameters().stream().filter(p -> !p.isDynamic()).count());
            aPluginConfiguration.logParams();

        } catch (PluginUtilsException e) {
            Assert.fail();
        }
    }

    /**
     * Update a {@link PluginConfiguration} : change the parameter's status from dynamic to not dynamic
     */
    @Test
    @Requirement("REGARDS_DSL_CMP_PLG_330")
    @Purpose("Change the parameter's status from not dynamic to dynamic.")
    public void updateParameterToDynamic() {
        final PluginConfiguration aPluginConfiguration = getPluginConfigurationWithDynamicParameter();
        aPluginConfiguration.setId(AN_ID);
        try {
            Mockito.when(pluginConfRepositoryMocked.exists(aPluginConfiguration.getId())).thenReturn(true);
            Mockito.when(pluginConfRepositoryMocked.findOne(aPluginConfiguration.getId()))
                    .thenReturn(aPluginConfiguration);
            Mockito.when(pluginConfRepositoryMocked.save(aPluginConfiguration)).thenReturn(aPluginConfiguration);

            PluginConfiguration updatedConf = pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);

            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(p -> p.isDynamic()).count(),
                                updatedConf.getParameters().stream().filter(p -> p.isDynamic()).count());
            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(p -> !p.isDynamic()).count(),
                                updatedConf.getParameters().stream().filter(p -> !p.isDynamic()).count());

            aPluginConfiguration.logParams();
            final List<PluginParameter> parameters = aPluginConfiguration.getParameters();
            for (final PluginParameter p : updatedConf.getParameters()) {
                if (!p.isDynamic()) {
                    parameters.remove(p);
                    p.setIsDynamic(true);
                    p.setDynamicsValues(Arrays.asList(new PluginDynamicValue("one"), new PluginDynamicValue("two"),
                                                      new PluginDynamicValue("three"), new PluginDynamicValue("for"),
                                                      new PluginDynamicValue("five"), new PluginDynamicValue("six")));
                    p.setValue(p.getDynamicsValues().get(0).getValue());
                    parameters.add(p);
                    break;
                }
            }

            aPluginConfiguration.setParameters(parameters);
            updatedConf = pluginServiceMocked.updatePluginConfiguration(aPluginConfiguration);

            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(p -> p.isDynamic()).count(),
                                updatedConf.getParameters().stream().filter(p -> p.isDynamic()).count());
            Assert.assertEquals(aPluginConfiguration.getParameters().stream().filter(p -> !p.isDynamic()).count(),
                                updatedConf.getParameters().stream().filter(p -> !p.isDynamic()).count());
            aPluginConfiguration.logParams();

        } catch (PluginUtilsException e) {
            Assert.fail();
        }
    }

}
