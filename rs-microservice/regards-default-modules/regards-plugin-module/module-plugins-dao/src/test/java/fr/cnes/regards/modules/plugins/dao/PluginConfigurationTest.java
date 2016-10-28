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

/***
 * Unit testing of {@link PluginConfiguration} persistence.
 *
 * @author cmertz
 *
 */

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { PluginDaoTestConfig.class })
@DirtiesContext
public class PluginConfigurationTest {

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
     * Security service to generate tokens.
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void createPluginConfiguration() {
        try {
            jwtService.injectToken(PluginDaoUtility.PROJECT, PluginDaoUtility.USERROLE);

            deleteAllFromRepository();

            final PluginConfiguration jpaConf = pluginConfigurationRepository
                    .save(PluginDaoUtility.getPluginConfigurationWithParameters());
            Assert.assertEquals(1, pluginConfigurationRepository.count());

            Assert.assertEquals(PluginDaoUtility.getPluginConfigurationWithParameters().getLabel(), jpaConf.getLabel());
            Assert.assertEquals(PluginDaoUtility.getPluginConfigurationWithParameters().getVersion(),
                                jpaConf.getVersion());
            Assert.assertEquals(PluginDaoUtility.getPluginConfigurationWithParameters().getPluginId(),
                                jpaConf.getPluginId());
            Assert.assertEquals(PluginDaoUtility.getPluginConfigurationWithParameters().getIsActive(),
                                jpaConf.getIsActive());
            Assert.assertEquals(PluginDaoUtility.getPluginConfigurationWithParameters().getPluginClassName(),
                                jpaConf.getPluginClassName());
            Assert.assertEquals(PluginDaoUtility.getPluginConfigurationWithParameters().getParameters().size(),
                                pluginParameterRepository.count());
            Assert.assertEquals(PluginDaoUtility.getPluginConfigurationWithParameters().getPriorityOrder(),
                                jpaConf.getPriorityOrder());
            PluginDaoUtility.getPluginConfigurationWithParameters().getParameters()
                    .forEach(p -> Assert.assertEquals(
                                                      PluginDaoUtility
                                                              .getPluginConfigurationWithParameters()
                                                              .getParameterConfiguration(p.getName()),
                                                      jpaConf.getParameterConfiguration(p.getName())));
        } catch (JwtException e) {
            Assert.fail(PluginDaoUtility.INVALID_JWT);
        }
    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void createAndFindPluginConfigurationWithParameters() {
        try {
            jwtService.injectToken(PluginDaoUtility.PROJECT, PluginDaoUtility.USERROLE);

            deleteAllFromRepository();

            // save a plugin configuration
            final PluginConfiguration aPluginConf = pluginConfigurationRepository
                    .save(PluginDaoUtility.getPluginConfigurationWithParameters());
            Assert.assertEquals(PluginDaoUtility.getPluginConfigurationWithParameters().getParameters().size(),
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
            Assert.fail(PluginDaoUtility.INVALID_JWT);
        }
    }

    /**
     * Unit test of creation {@link PluginConfiguration}
     */
    @Test
    public void updatePluginConfigurationWithParameters() {
        try {
            jwtService.injectToken(PluginDaoUtility.PROJECT, PluginDaoUtility.USERROLE);

            deleteAllFromRepository();

            // save a plugin configuration
            final PluginConfiguration aPluginConf = pluginConfigurationRepository
                    .save(PluginDaoUtility.getPluginConfigurationWithParameters());

            // set two new parameters to the plugin configuration
            aPluginConf.setParameters(Arrays.asList(PluginDaoUtility.PARAMETER2));

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

            PluginDaoUtility.INTERFACEPARAMETERS.forEach(p -> pluginParameterRepository.delete(p));
            Assert.assertEquals(aPluginConf.getParameters().size(), pluginParameterRepository.count());

        } catch (JwtException e) {
            Assert.fail(PluginDaoUtility.INVALID_JWT);
        }
    }

    private void deleteAllFromRepository() {
        pluginConfigurationRepository.deleteAll();
        pluginParameterRepository.findAll().forEach(p -> pluginParameterRepository.delete(p));
        PluginDaoUtility.resetId();
    }

}
