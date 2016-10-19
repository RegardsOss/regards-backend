/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.dao;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;

/***
 * {@link PluginParameter} unit testing domain persistence
 *
 * @author cmertz
 *
 */

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { PluginDaoTestConfig.class })
@DirtiesContext
public class PluginConfigurationTest extends PluginDaoTestDataUtility {

    /**
     * IPluginConfigurationRepository
     */
    @Autowired
    private IPluginConfigurationRepository pluginConfigurationRepository;

    /**
     * IPluginParameterRepository
     */
    @Autowired
    private IPluginParameterRepository pluginParameterRepository;

    /**
     * IPluginDynamicValueRepository
     */
    @Autowired
    private IPluginDynamicValueRepository pluginDynamicValueRepository;

    /**
     * Security service to generate tokens.
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @DirtiesContext
    @Test
    public void createPluginConfiguration() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);

            deleteAllFromRepository();

            final PluginConfiguration jpaConf = pluginConfigurationRepository
                    .save(getPluginConfigurationWithParameters());
            Assert.assertEquals(1, pluginConfigurationRepository.count());

            Assert.assertEquals(getPluginConfigurationWithParameters().getLabel(), jpaConf.getLabel());
            Assert.assertEquals(getPluginConfigurationWithParameters().getVersion(), jpaConf.getVersion());
            Assert.assertEquals(getPluginConfigurationWithParameters().getPluginId(), jpaConf.getPluginId());
            Assert.assertEquals(getPluginConfigurationWithParameters().getIsActive(), jpaConf.getIsActive());
            Assert.assertEquals(getPluginConfigurationWithParameters().getPluginClassName(),
                                jpaConf.getPluginClassName());
            Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                                pluginParameterRepository.count());
            Assert.assertEquals(getPluginConfigurationWithParameters().getPriorityOrder(), jpaConf.getPriorityOrder());
            getPluginConfigurationWithParameters().getParameters()
                    .forEach(p -> Assert
                            .assertEquals(getPluginConfigurationWithParameters().getParameterConfiguration(p.getName()),
                                          jpaConf.getParameterConfiguration(p.getName())));
        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @DirtiesContext
    @Test
    public void createAndFindPluginConfigurationWithParameters() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);

            deleteAllFromRepository();

            // save a plugin configuration
            final PluginConfiguration aPluginConf = pluginConfigurationRepository
                    .save(getPluginConfigurationWithParameters());
            Assert.assertEquals(getPluginConfigurationWithParameters().getParameters().size(),
                                pluginParameterRepository.count());
            Assert.assertEquals(1, pluginConfigurationRepository.count());

            // find it
            final PluginConfiguration jpaConf = pluginConfigurationRepository.findOne(aPluginConf.getId());

            // compare the initial conf with the results of the search
            Assert.assertEquals(aPluginConf.getLabel(), jpaConf.getLabel());
            Assert.assertEquals(aPluginConf.getVersion(), jpaConf.getVersion());
            Assert.assertEquals(aPluginConf.getPluginId(), jpaConf.getPluginId());
            Assert.assertEquals(aPluginConf.getIsActive(), jpaConf.getIsActive());
            Assert.assertEquals(aPluginConf.getPluginClassName(), jpaConf.getPluginClassName());
            Assert.assertEquals(aPluginConf.getParameters().size(), pluginParameterRepository.count());
            Assert.assertEquals(aPluginConf.getPriorityOrder(), jpaConf.getPriorityOrder());
            aPluginConf.getParameters()
                    .forEach(p -> Assert.assertEquals(aPluginConf.getParameterConfiguration(p.getName()),
                                                      jpaConf.getParameterConfiguration(p.getName())));
        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @DirtiesContext
    @Test
    public void updatePluginConfigurationWithParameters() {
        try {
            jwtService.injectToken(PROJECT, USERROLE);

            deleteAllFromRepository();

            // save a plugin configuration
            final PluginConfiguration aPluginConf = pluginConfigurationRepository
                    .save(getPluginConfigurationWithParameters());

            // set two new parameters to the plugin configuration
            aPluginConf.setParameters(Arrays.asList(PARAMETER1, PARAMETER2));

            // update the plugin configuration
            final PluginConfiguration jpaConf = pluginConfigurationRepository.save(aPluginConf);

            Assert.assertEquals(1, pluginConfigurationRepository.count());

            // compare the initial conf with the results of the search
            Assert.assertEquals(aPluginConf.getLabel(), jpaConf.getLabel());
            Assert.assertEquals(aPluginConf.getVersion(), jpaConf.getVersion());
            Assert.assertEquals(aPluginConf.getPluginId(), jpaConf.getPluginId());
            Assert.assertEquals(aPluginConf.getIsActive(), jpaConf.getIsActive());
            Assert.assertEquals(aPluginConf.getPluginClassName(), jpaConf.getPluginClassName());
            Assert.assertEquals(aPluginConf.getPriorityOrder(), jpaConf.getPriorityOrder());
            aPluginConf.getParameters()
                    .forEach(p -> Assert.assertEquals(aPluginConf.getParameterConfiguration(p.getName()),
                                                      jpaConf.getParameterConfiguration(p.getName())));

            INTERFACEPARAMETERS.forEach(p -> pluginParameterRepository.delete(p));
            Assert.assertEquals(aPluginConf.getParameters().size(), pluginParameterRepository.count());

        } catch (JwtException e) {
            Assert.fail(INVALID_JWT);
        }
    }

    private void deleteAllFromRepository() {
        pluginConfigurationRepository.deleteAll();
        pluginParameterRepository.deleteAll();
        pluginDynamicValueRepository.deleteAll();
        resetId();
    }

}
